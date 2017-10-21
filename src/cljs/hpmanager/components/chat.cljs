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

(defn chat-page
  "A single page for a single chat"
  [chat-id user-set]
  (let [c (rf/subscribe [::chat-page chat-id user-set])]
    (fn []
      [:div.tab-pane.fade.active.in
       ;; Display messages
       (->> @c
            (map mes/format-message)
            (interpose [:br]))
       ;; TODO chat bar
       ])))
(defn single-chat-component
  "A single chat window that shows all messages"
  []
  (let [c (rf/subscribe [::chat-page-all])]
    (fn []
      (-> @c
          (take 50)
          (map mes/format-message)
          (reverse)
          (interpose [:br])))))
(comment
; (defn chat-component
;   [chat-id]
;   (let [c (rf/subscribe [::chat chat-id])]
;     (fn []
;       [:div.nav.nav-tabs
;        (map (fn [user-set] [li.
)
