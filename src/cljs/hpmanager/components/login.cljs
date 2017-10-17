(ns hpmanager.components.login
  (:require
    [hpmanager.sockets :as sockets]
    [hpmanager.components.shared :as cs]
    [reagent.core :as r]
    [taoensso.timbre :as log]
    [re-frame.core :as rf :refer [dispatch reg-event-db]]
    )
  )

(defn login-component
  []
  (let [details (rf/subscribe [:login])]
    (fn []
      [cs/collapsable
       "Login:"
       [:input.form-control {:type "text"
                             :placeholder "Username"
                             ;:value (:uname @details)
                             :on-change #(let [new-uname (-> % .-target .-value)]
                                               (rf/dispatch [::login-set-username new-uname]))}]
       [:input.form-control {:type "password"
                             :placeholder "Password"
                             ;:value (:pass @details)
                             :on-change #(rf/dispatch [::login-set-password (-> % .-target .-value)])}]
       (cs/button #(rf/dispatch [::login-attempt-login]) "Login")
       ])))

(reg-event-db
  ::login-set-username
  (fn [db [_ uname]]
    (assoc-in db [:login :uname] uname)))

(reg-event-db
  ::login-set-password
  (fn [db [_ pass]]
    (assoc-in db [:login :pass] pass)))

(reg-event-db
  ::login-attempt-login
  (fn [{{:keys [uname pass]} :login :as db} _]
    (if (and uname (< 0 (count uname)) pass (< 0 (count pass)))
      (do
        (sockets/login! (get-in db [:login :uname])
                        (get-in db [:login :pass]))
        (-> db
            (assoc-in [:login :pass] ""))) ; Reset the password
      (do (js/alert "Please enter a username or password")
          db))))
