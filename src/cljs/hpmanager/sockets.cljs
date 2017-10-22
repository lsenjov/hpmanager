(ns hpmanager.sockets
  "Sockets and state management"
  (:require
    [clojure.string :as str]
    [cljs.core.async :as async  :refer (<! >! put! chan)]
    [taoensso.encore :as encore :refer-macros (have have?)]
    [taoensso.timbre :as log :refer-macros (tracef debugf infof warnf errorf)]
    [taoensso.sente  :as sente  :refer (cb-success?)]
    [re-frame.core :as rf :refer [dispatch reg-event-db]]

    ;; Optional, for Transit encoding:
    [taoensso.sente.packers.transit :as sente-transit])
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

;;;; Util for logging output to on-screen console
; TODO remove/change to currently used logging library

(def output-el (.getElementById js/document "output"))
(defn ->output! [fmt & args]
  (let [msg (apply encore/format fmt args)]
    (log/info msg)
    ;(aset output-el "value" (str "• " (.-value output-el) "\n" msg))
    ;(aset output-el "scrollTop" (.-scrollHeight output-el))
    ))

(->output! "ClojureScript appears to have loaded correctly.")
;;;; Define our Sente channel socket (chsk) client

(let [rand-chsk-type :auto

      ;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/chsk" ; Must match server Ring routing URL
        {:type   rand-chsk-type
         :packer packer})]

  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (do
    (->output! "Received packet of type: %s" id)
    (-event-msg-handler ev-msg)))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (->output! "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (->output! "Channel socket successfully established!: %s" new-state-map)
      (->output! "Channel socket state change: %s"              new-state-map))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (->output! "Push event from server: %s" ?data)
  (when ?data
    (rf/dispatch ?data)))

;; When we've reconnected
(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Handshake: %s" ?data))
  (rf/dispatch [:refresh :all]))

(rf/reg-event-db
  :chsk/ws-ping
  (fn [db _]
    (log/tracef "ws-ping from server")
    db))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-client-chsk-router!
      ch-chsk event-msg-handler)))
(defn start! [] (start-router!))
(defonce _start-once (start!))

;;;; Begin userspace functions here
(defn login!
  "Logs the user in. Re-connects the websocket if valid.
  Returns true on success, else false."
  [user-id password]
  (->output! "Logging in with user-id %s" user-id)
  (sente/ajax-lite "/login"
                   {:method :post
                    :headers {:X-CSRF-Token (:csrf-token @chsk-state)}
                    :params {:user-id (str user-id)
                             :password (str password)}
                    :resp-type :edn}
                   (fn [{:keys [success? ?status ?error ?content ?content-type] :as ajax-resp}]
                     (->output! "Ajax login response: %s" ajax-resp)
                     (let [login-success? (get-in ajax-resp [:?content])]
                       (dispatch [::set-login-status login-success?])
                       (if-not login-success?
                         (->output! "Login failed")
                         (do
                           (->output! "Login Successful")
                           (sente/chsk-reconnect! chsk)))))))

(reg-event-db
  ::set-login-status
  (fn [db [_ status]]
    (-> db
        (assoc-in [:login :status] status)
        (assoc db :page :chat))))
(reg-event-db
  :logout
  (fn [db [_ status]]
    (-> db
        (assoc-in [:login :status] nil)
        (assoc :page :home))))
(reg-event-db
  :refresh
  (fn [db [_ request]]
    (log/debugf "Requesting full refresh from server.")
    (chsk-send! [:state/refresh request])
    db))
