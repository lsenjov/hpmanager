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
        ; Text to send
        t (r/atom "")]
    (fn []
      (let [dispatch-fn #(do (rf/dispatch
                               [::send-message
                                (mes/construct-message chat-id
                                                       (:uid @u)
                                                       user-set
                                                       @t)])
                             (reset! t "")
                             (.preventDefault %) ;; Stop it from actually going anywhere
                             js/false)]
        [:div
         [:div.tab-pane
          ;[:div "Recipients: "
          ; (if (= :everyone user-set)
          ;   "Everyone"
          ;   (->> user-set (map str) sort (interpose ", ") (apply str)))]
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
          "Send Message"]
         ]
        ))))
(defn chat-tab
  [chat-id recipients]
  (let [sw (rf/subscribe [::cs/tab-switch chat-id recipients])]
    (fn []
      [(if (= recipients @sw) :li.active :li)
       [chat-page chat-id recipients]])))
(defn create-new-conversation-tab
  "Allows the user to select a number of users to send a message to"
  [chat-id]
  (let [c (rf/subscribe [::chat-users chat-id])
        l (rf/subscribe [:login-status])
        n (r/atom #{})]
    (fn []
      [:div
       (pr-str @n)
       [:div.checkbox
        (map (fn [u] ^{:key u}
               [:label
                [:input {:type "checkbox"
                         :onChange #(swap! n (if (-> % .-target .-checked) conj disj) u)
                         }]
                u])
             (disj @c (:uid @l) :taoensso.sente/nil-uid))
        ]
       [cs/button (fn [] (rf/dispatch [::send-message
                                       (mes/construct-message chat-id
                                                              (:uid @l)
                                                              (conj @n (:uid @l))
                                                              "Conversation opened")]))
        "Open Conversation"]
       ])))

(defn- remove-self
  "Removes self from sets of recipients"
  [s]
  (if (set? s)
    (disj s (:uid (deref (rf/subscribe [:login-status]))))
    s))
(defn prettify-col
  [s]
  (if (coll? s)
    (->> s sort (interpose ", ") (apply str))
    s))
(defn full-chat-page
  "A tabbed page for a single chat id"
  [chat-id]
  (let [c (rf/subscribe [::chat-categories chat-id])]
    (fn []
      [:div
       [cs/tabbed
        chat-id
        (apply merge {}
               ^{:key "NewConvo"} {"New Conversation" (partial create-new-conversation-tab chat-id)}
               (doall (map (fn [recipients]
                             ^{:key recipients}
                             {(-> recipients remove-self prettify-col)
                              (partial chat-page chat-id recipients)})
                           (conj @c :everyone)))
               )]])))

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
(rf/reg-sub
  ;; Returns a list of users in this chat
  ::chat-users
  (fn [db [_ chat-id]]
    (mes/get-users db chat-id)))
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
    (let [refresh-fn (->> chats
                          (map (fn [chat] (fn [m] (mes/refresh-chat m chat))))
                          (apply comp)
                          )]
      (refresh-fn db)
      )))

(comment
  )
