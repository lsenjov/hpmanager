(ns hpmanager.state
  "This is the local storage for all state. If something on the server changes, it goes here."
  (:require [hpmanager.routes.sockets :as sockets :refer [chsk-send! connected-uids]]
            [taoensso.timbre :as log]
            [hpmanager.model.messaging :as mes]
            )
  )

;; TODO init global state
"Global state lives here"
(defonce global-state
  (atom
    (-> {}
        (mes/new-module)
        )))

(comment
  (mes/get-messages @global-state "global" :everyone)
  (mes/get-categories @global-state "global")
  (deref global-state)
  (-> @global-state ::mes/module ::mes/chats (get "global") ::mes/queue
      (->> (map ::mes/message-recipients))
      distinct
      )
  )

(def refresh-handlers
  "Map of keyword to function.
  Each function operates on a deref'd global state, and returns a vector of data to
  send to the client.
  Each function takes two arguments, the state, and the calling user"
  {::mes/refresh (fn [state uid]
                [::mes/refresh (mes/get-messages-for-refresh state uid)])})

(defmethod sockets/-event-msg-handler
  :chsk/uidport-open ; New user connecting (NOT a second of the same user connecting)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  ;; TODO add initial subscriptions
  (log/infof "First client of user %s connecting" ?data)
  ; Add them to the global chat channel
  (swap! global-state mes/user-join mes/global-chat-channel ?data)
  )
(defmethod sockets/-event-msg-handler
  :chsk/uidport-close ; Last client of a user is disconnecting
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/infof "Last client of user %s disconnecting" ?data)
  nil)

(defn get-refresh-data
  "Returns a vector of kw/data pairs to send back"
  [kws uid]
  (let [kws-set (set kws)
        state @global-state]
    (->> refresh-handlers
         (filter (comp kws-set key))
         (map (fn [[_ f]] (f state uid)))
         )))

;; Socket handlers
(defmethod sockets/-event-msg-handler
  ::mes/send
  [{:as ev-msg
    {:as ?data ::mes/keys [chat-id message-content message-sender message-recipients]} :?data
    :keys [event id ring-req ?reply-fn send-fn]}]
  (if (and (string? message-content)
           (pos? (count message-content))
           (string? message-sender)
           (pos? (count message-content)))
    (let [message (mes/add-time ?data)]
      (log/debugf "Received message %s from %s to %s" message-content message-sender message-recipients)
      (doseq [uid (if (= :everyone message-recipients) ; TODO filter by chat-id
                    (mes/get-users @global-state chat-id)
                    message-recipients)]
        (chsk-send! uid [::mes/recv message])))
    (log/errorf "Received a message, invalid form: %s" ?data)))

(defmethod sockets/-event-msg-handler
  :state/refresh
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (log/infof "state/refresh. ev-msg: %s" ev-msg)
  (log/infof "uid is: %s" (:uid ev-msg))
  (let [ret-data
        (cond
          (= :all ?data)
          (get-refresh-data (keys refresh-handlers) uid)

          (coll? ?data)
          (get-refresh-data ?data uid)

          :else ; returns nil
          (log/errorf "Got request for state refresh, invalid request: %s" ?data))]
    (log/infof "Return data is: %s" (vec ret-data))
    (doseq [me ret-data]
      (log/infof "Returning single data: %s, type: %s" me (type me))
      (apply chsk-send! uid ret-data)
      )
    ret-data))
