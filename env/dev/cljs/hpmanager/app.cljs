(ns ^:figwheel-no-load hpmanager.app
  (:require [hpmanager.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
