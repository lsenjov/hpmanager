(ns hpmanager.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [hpmanager.core-test]))

(doo-tests 'hpmanager.core-test)

