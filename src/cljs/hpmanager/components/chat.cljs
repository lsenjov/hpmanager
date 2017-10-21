(ns hpmanager.components.chat
  "Generic chat interface"
  (:require
    [taoensso.timbre :as log]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [hpmanager.sockets :as sockets]
    [hpmanager.model.messaging :as mes]
    )
  )

(def global-chat-channel "global")
(defn chat-page
  "A single page for a single chat"
  [chat-id user-set]
  (let [c (rf/subscribe [::chat-page chat-id user-set 20])
        u (rf/subscribe [:login-status])
        t (r/atom "")]
    (fn []
      [:div
       [:div.tab-pane.fade.active.in
        ;; Display messages
        (->> @c
             (map (fn [m] ^{:key m} [:div (mes/format-message m)]))
             (reverse))
        [:div>input {:type "text"
                     :value @t
                     :on-change #(reset! t (-> % .-target .-value))}]
        ]
       [:div.btn.btn-default {:on-click #(do (rf/dispatch
                                               [::send-message
                                                (mes/construct-message global-chat-channel
                                                                       (:uid @u)
                                                                       :everyone
                                                                       @t)])
                                             (reset! t ""))}
        "DEBUG add message"]]
       )))
(defn single-chat-component
  "A single chat window that shows all messages"
  [chat-id]
  (let [c (rf/subscribe [::chat-page chat-id :everyone])]
    (fn []
      (->> @c
           (take 20)
           (map mes/format-message)
           (reverse)
           (interpose [:br])))))

(rf/reg-sub
  ::chat-page
  (fn [db [_ chat-id user-set ?n]]
    (if ?n
      (mes/get-messages db chat-id user-set ?n)
      (mes/get-messages db chat-id user-set))))
(rf/reg-event-db
  ::send-message
  (fn [db [_ message]]
    (sockets/chsk-send! [::mes/send message])
    db))
(rf/reg-event-db
  ::mes/recv
  (fn [db [_ message]]
    (mes/add-message db message)))
(comment
; (defn chat-component
;   [chat-id]
;   (let [c (rf/subscribe [::chat chat-id])]
;     (fn []
;       [:div.nav.nav-tabs
;        (map (fn [user-set] [li.
)
