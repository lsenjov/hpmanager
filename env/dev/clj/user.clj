(ns user
  (:require [mount.core :as mount]
            [hpmanager.figwheel :refer [start-fw stop-fw cljs]]
            hpmanager.core))

(defn start []
  (mount/start-without #'hpmanager.core/repl-server))

(defn stop []
  (mount/stop-except #'hpmanager.core/repl-server))

(defn restart []
  (stop)
  (start))


