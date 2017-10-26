(ns hpmanager.sync
  "Manages syncing an object in the serverside database,
  with a user modifying it client side"
  (:require
    #?@(:clj
         [[codax.core :refer :all]

          [hpmanager.shared :as shared]

          [hpmanager.db.core :as db :refer [*db*]]
          [hpmanager.routes.sockets :as sockets]]
         :cljs
         [[re-frame.core :as rf]

          [hpmanager.shared :as shared]

          [hpmanager.sockets :as sockets]]
       )))

;; The client requests an object from the database (or to create one if none exist)
;; The server grabs it and returns the path.
;; Whenever the object is updated, the client requests the server modify the specified value

#?(
:cljs
(do
  (rf/reg-sub
    ::sync-component
    (fn [db [_ path]]
      (get-in db (vec (concat [::sync] [path])))))
  (rf/reg-event-db
    ;; When it receives an event that the server has synchronised an object, only update it locally
    ::server-sync
    (fn [db [_ path i]]
      (assoc-in db path i)))
  (rf/reg-event-db
    ;; When it needs to get a file from the database to initially open
    ::sync-open
    (fn [db [_ path]]
      (get-in db path)))
  (rf/reg-event-db
    ;; When an object locally changes, update it
    ::sync-component
    (fn [db [_ path i]]
      (assoc-in db path i)
      (sockets/chsk-send! [::sync-component path i]) ; Sync with server, send only
      ))
  (defn sync-atom
    "Returns an atom. Whenever the atom is changed,
    synchonises the local database and the external database"
    [path]
    (let [obj (rf/subscribe [::sync-component path])]
      ;; TODO check that this works. I'm not sure how re-frame does it's subscriptions
      ;; Hopefully re-frame just uses a straight reset! on the subscribed atoms, in which case
      ;; everything should be perfectly okay
      (add-watch obj ::sync
                 (fn [_ _ old new]
                   (if (not= old new)
                     (rf/dispatch [::sync-component path new]))))
      obj))
)

:clj
(do
  (defmethod sockets/-event-msg-handler
    ::sync-component
    [{:as ev-msg :keys [event id ring-req ?reply-fn send-fn]
      [[path data :as data]] :?data}]
    (assoc-at! *db* path data))
  (defmethod sockets/-event-msg-handler
    ::sync-open
    [{:as ev-msg :keys [event id ring-req ?reply-fn send-fn]
      [[path data :as data]] :?data}]
    (sockets/chsk-send! [::server-sync path (get-at! *db* path)]))


))
