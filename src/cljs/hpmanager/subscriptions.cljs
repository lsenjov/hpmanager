(ns hpmanager.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(reg-sub
  :login
  (fn [db _]
    (:login db)))

(reg-sub
  :login-status
  (fn [db _]
    (get-in db [:login :status])))

(reg-sub
  :debug
  (fn [db _]
    (dissoc db :docs)))
