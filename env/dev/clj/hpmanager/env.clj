(ns hpmanager.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [hpmanager.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[hpmanager started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[hpmanager has shut down successfully]=-"))
   :middleware wrap-dev})
