(ns hpmanager.components.shared
  "A library of helper wrappers for components"
  (:require
    [taoensso.timbre :as log]
    [reagent.core :as r]
    )
  )

(def button-size (atom "btn-sm"))

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
  "Creates a collapsable panel."
  [header-text & body-items]
  (let [collapsed? (r/atom false)]
    (fn []
    [:div.panel.panel-default
     [:div.panel-heading
      [:div.btn.btn-default.btn-xs {:on-click #(do (log/infof "Collapsing/expanding: %s" header-text)
                                            (swap! collapsed? not))}
       (if @collapsed? "+" "-")]
      \space header-text]
     (if-not @collapsed? (apply vector :div.panel-body body-items))
     ])))

