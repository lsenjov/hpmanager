(ns hpmanager.model.shared
  (:require [clojure.spec.alpha :as s]
            )
  )

(def rank-to-clearance
  {0 "IR" 1 "R" 2 "O" 3 "Y" 4 "G" 5 "B" 6 "I" 7 "V" 8 "U"})
(def clearance-to-rank
  (->> rank-to-clearance
       (map (fn [[k v]] {v k}))
       (apply merge {})))
(defn promote
  [c]
  (-> c
      clearance-to-rank
      inc
      (min 8)
      rank-to-clearance))
(defn demote
  [c]
  (-> c
      clearance-to-rank
      dec
      (max 0)
      rank-to-clearance))
(comment
  (promote "IR")
  (promote "U")
  (demote "IR")
  (demote "U"))
