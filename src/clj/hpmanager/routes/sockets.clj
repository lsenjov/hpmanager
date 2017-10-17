(ns hpmanager.routes.sockets
  (:require [hpmanager.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.middleware.defaults]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [hpmanager.config :refer [env]]
            [hpmanager.db.core :as db]
            [mount.core :as mount]
            [taoensso.sente :as sente]
            ;[taoensso.sente.server-adapters.immutant      :refer (get-sch-adapter)]
            [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]
            ))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter)
                                  {:packer :edn})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(comment
  (deref connected-uids)
  (chsk-send! "TestName" [:unknown/asdf {:hi "Hi!"}])
  )

(defn- diff-set-one-way
  [c old new]
  (->> new
       (remove old)
       (map (partial str c))))
(defn- diff-set
  "Takes two sets of strings. It prints the strings that changed, with a
  preceeding + or - sign if the string was added or left"
  [old new]
  (->>
    (concat (diff-set-one-way \+ old new)
            (diff-set-one-way \- new old))
    (interpose ", ")
    (apply str)
    )
  )
(comment
  (diff-set-one-way \+ #{"a" "b"} #{"a" "b" "c"})
  (diff-set-one-way \+ #{"a" "b" "c"} #{"a" "b" "c"})
  (diff-set-one-way \+ #{} #{"a" "b" "c"})

  (diff-set #{"a" "b"} #{"a" "b" "c"})
  (diff-set #{"a" "b"} #{})
  )

;; For debug purposes, log when the connected users change
(add-watch connected-uids ::connected-uids
           (fn [_ _ old new]
             (when (not= old new)
               (log/debugf "Connected uids changed: %s, currently connected: %s" (diff-set (:any old) (:any new)) (:any new)))))

(defn login-handler
  "Test that a user's key-val pair are correct."
  [ring-req]
  (let [{:keys [session params]} ring-req
        {:keys [user-id password]} params]
    (log/debugf "Login request from user: %s" user-id)
    (log/tracef "Login request in full: %s" ring-req)
    (if-let [u (db/login-user user-id password)]
      {:status 200
       :body (pr-str {:uid user-id})
       :resp-type :edn
       :session (assoc session :uid user-id)}
      {:status 200
       :body (pr-str {:error "Invalid Username or Password"})
       :resp-type :edn
       :session session})))

(defroutes sockets-routes-inner
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (POST "/login" req (login-handler                req))
  )

(def sockets-routes
  (-> sockets-routes-inner
      ;; Add necessary Ring middleware:
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )
(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )
(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (log/debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))
(defmethod -event-msg-handler
  :chsk/ws-ping ; We ignore pings
  [_]
  nil)
(defmethod -event-msg-handler
  :util/ping ; Echos whatever was sent, back
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (do
    (let [uid (-> ring-req :session :uid)]
      (log/infof "Got a ping from %s!" uid)
      (if ?reply-fn
        (do (log/info "Using reply-fn")
            (?reply-fn {:util/pong "Pong!"}))
        (do (log/info "Using send")
            (send-fn uid [:util/pong "Pong!"]))))))

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))

;;;; Init stuff

(defn stop!  []  (stop-router!))
(defn start! [] (start-router!))

(mount/defstate init-sockets
  :start (start!)
  :stop (stop!))
(defonce _start_once (mount/start #'init-sockets))

(comment
  (start!)
  )
