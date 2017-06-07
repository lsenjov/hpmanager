(ns hpmanager.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[hpmanager started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[hpmanager has shut down successfully]=-"))
   :middleware identity})
