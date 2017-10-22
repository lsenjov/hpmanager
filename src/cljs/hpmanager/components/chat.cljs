(ns hpmanager.components.chat
  "Generic chat interface"
  (:require
    [taoensso.timbre :as log]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [hpmanager.sockets :as sockets]
    [hpmanager.model.messaging :as mes]
    [hpmanager.components.shared :as cs]
    )
  )

(def global-chat-channel mes/global-chat-channel)
(defn chat-page
  "A single page for a single chat"
  [chat-id user-set]
  (let [c (rf/subscribe [::chat-page chat-id user-set 20])
        u (rf/subscribe [:login-status])
        t (r/atom "")]
    (fn []
      (let [dispatch-fn #(do (rf/dispatch
                               [::send-message
                                (mes/construct-message chat-id
                                                       (:uid @u)
                                                       :everyone
                                                       @t)])
                             (reset! t "")
                             (.preventDefault %) ;; Stop it from actually going anywhere
                             js/false)]
      [:div
       [:div.tab-pane
        [:div "Recipients: "
         (if (= :everyone user-set)
           "Everyone"
           (->> user-set (interpose ", ")))]
        ;; Display messages
        [:div
         (->> @c
              (map (fn [m] ^{:key m} [:div (mes/format-message m)]))
              (reverse))]
        [:form {:onSubmit dispatch-fn}
         [:div>input.form-control
          {:type "text"
           :value @t
           :on-change #(reset! t (apply str (take 140 (-> % .-target .-value))))
           :placeholder "Type message, press enter to send"}]
         ]
        ]
       [cs/button dispatch-fn
        "Send Message"]] ;; TODO send on enter
       ))))
(defn chat-tab
  [chat-id recipients]
  (let [sw (rf/subscribe [::cs/tab-switch chat-id recipients])]
    (fn []
      [(if (= recipients @sw) :li.active :li)
       [chat-page chat-id recipients]])))
(defn full-chat-page
  "A tabbed page for a single chat id"
  [chat-id]
  (let [c (rf/subscribe [::chat-categories chat-id])]
    (fn []
      (log/infof "full-chat-page. chat-id: %s c: %s" chat-id @c)
      [cs/tabbed
       chat-id
       (reduce merge {}
               (map (fn [recipients]
                      ^{:key recipients}
                      {recipients (partial chat-page chat-id recipients)})
                    (conj @c :everyone)))])))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Subscriptions
(rf/reg-sub
  ::chat-page
  (fn [db [_ chat-id user-set ?n]]
    (if ?n
      (mes/get-messages db chat-id user-set ?n)
      (mes/get-messages db chat-id user-set))))
(rf/reg-sub
  ;; Returns the different chat categories from a chat
  ::chat-categories
  (fn [db [_ chat-id]]
    (mes/get-categories db chat-id)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Events
(rf/reg-event-db
  ::send-message
  (fn [db [_ message]]
    (sockets/chsk-send! [::mes/send message])
    db))
(rf/reg-event-db
  ::mes/recv
  (fn [db [_ message]]
    (mes/add-message db message)))
(rf/reg-event-db
  ::mes/refresh
  (fn [db [_ chats]]
    (log/infof "Received refresh data: %s" chats)
    (let [refresh-fn (->> chats
                          (map (fn [chat] (log/infof "Chat is: %s" chat) (fn [m] (mes/refresh-chat m chat))))
                          (apply comp)
                          )]
      (refresh-fn db)
      )))

(comment
  )
