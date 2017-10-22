(ns hpmanager.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [hpmanager.ajax :refer [load-interceptors!]]
            [hpmanager.handlers]
            [hpmanager.subscriptions]
            [hpmanager.sockets :as socket]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]

            [hpmanager.components.shared :as cs]
            [hpmanager.components.chat :as chat]
            [hpmanager.components.login :as login]
            )
  (:import goog.History))

(defn nav-link [uri title page]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri}
      title]]))

(defn- display-name []
  (let [login-atom (rf/subscribe [:login-status])]
    (fn []
      [:span
      (if (:uid @login-atom)
        (str "Welcome Citizen " (:uid @login-atom))
        "Not logged in.")
      ])))

(defn navbar []
  (let [login-atom (rf/subscribe [:login-status])]
    (fn []
      [:nav.navbar.navbar-dark.bg-primary
       (if-not (:uid @login-atom) ; Is the user logged in?
         [:div.navbar-toggleable-xs
          [:a.navbar-brand {:href "#/"} "HPManager"]
          [:ul.nav.navbar-nav
           [nav-link "#/" "Login" :home]
           (:uid @login-atom)]]
         [:div.collapse.navbar-toggleable-xs
          [:a.navbar-brand {:href "#/"} "hpmanager"]
          [:ul.nav.navbar-nav
           [nav-link "#/" "Login" :home]
           [nav-link "#/chat" "Chat" :about]
           [:li.nav-item>a.nav-link {:href "#/"
                                     :onClick #(rf/dispatch [:logout])}
            "Logout " (:uid @login-atom)]]])])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn global-chat-page []
  [:div.container
   [cs/collapsable "Chat" [chat/full-chat-page chat/global-chat-channel]]])

(defn login-page []
  [:div.container
   [login/login-component]])

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      (cs/button #(socket/chsk-send! [:util/ping {:ping "Ping!"}] 5000) "Ping!")
      [cs/collapsable "Debug" [cs/debug-display]]
      [cs/button #(rf/dispatch [:refresh :all]) "Refresh all"]
      [cs/collapsable "Document:"
                      [:div {:dangerouslySetInnerHTML
                             {:__html (md->html docs)}}]]])])

(def pages
  {:home #'login-page
   :chat #'global-chat-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))
(secretary/defroute "/chat" []
  (rf/dispatch [:set-active-page :chat]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
