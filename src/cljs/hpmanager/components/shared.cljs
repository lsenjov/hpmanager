(ns hpmanager.components.shared
  "A library of helper wrappers for components"
  (:require
    [taoensso.timbre :as log]
    [re-frame.core :as rf]
    [reagent.core :as r]
    )
  )

(def button-size (atom "btn-sm"))

(defn debug-display
  "Displays the *entire* db map. Should be removed during debugging."
  []
  (let [debug-atom (rf/subscribe [:debug])]
    [:span (pr-str @debug-atom)]))

(defn button
  "Creates a button, re-sizes if button-size is changed"
  ([?state-map ?on-click-fn text]
   [:div.btn.btn-default
    (merge {:on-click ?on-click-fn
            :class @button-size}
           ?state-map)
    text])
  ([?on-click-fn text]
   (button nil ?on-click-fn text)))

(defn collapsable
  "Creates a collapsable panel. Each component should have a different header-text"
  [header-text & body-items]
  (let [collapsed? (rf/subscribe [::collapsed header-text])]
    (fn []
    [:div.panel.panel-default
     [:div.panel-heading
      [:div.btn.btn-default.btn-xs {:on-click #(do (log/infof "Collapsing/expanding: %s" header-text)
                                            (rf/dispatch [::toggle-maximised header-text]))}
       (if @collapsed? "+" "-")]
      \space header-text]
     (if-not @collapsed? (apply vector :div.panel-body body-items))
     ])))

(rf/reg-sub
  ::collapsed
  (fn [db [_ kw]]
    (get-in db [::collapsed kw])))

(rf/reg-event-db
  ::toggle-maximised
  (fn [db [_ kw]]
    (update-in db [::collapsed kw] not)))
