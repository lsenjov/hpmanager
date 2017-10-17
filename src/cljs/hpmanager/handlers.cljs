(ns hpmanager.handlers
  (:require [hpmanager.db :as db]
            [hpmanager.sockets :as sockets]
            [re-frame.core :refer [dispatch reg-event-db]]
            [taoensso.timbre :as log]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

