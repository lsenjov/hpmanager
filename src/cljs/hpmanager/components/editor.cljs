(ns hpmanager.components.editor
  (:require
    [taoensso.timbre :as log]
    [re-frame.core :as rf]
    [reagent.core :as r]

    [hpmanager.components.shared :as cs]
    [hpmanager.sync :as syn]
    )
  )

(defn edit-single-text
  "Creates a text box, that editing will sync the object to the server"
  [path]
  [:input.form-control])
