(ns hpmanager.model.shared
  (:require [clojure.spec.alpha :as s]
            )
  )

(def root-kws
  "A set detailing all the possible root object types of the db"
  #{::characters ::crises})

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
;; Stolen directly from the cljs source, because I can't get it to reference locally TODO
(defn- random-uuid []
  (letfn [(hex [] (.toString (rand-int 16) 16))]
    (let [rhex (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 16))) 16)]
      (uuid
        (str (hex) (hex) (hex) (hex)
             (hex) (hex) (hex) (hex) "-"
             (hex) (hex) (hex) (hex) "-"
             "4"   (hex) (hex) (hex) "-"
             rhex  (hex) (hex) (hex) "-"
             (hex) (hex) (hex) (hex)
             (hex) (hex) (hex) (hex)
             (hex) (hex) (hex) (hex))))))

(defn gen-uuid
  "Generates a random uuid value"
  []
  #?(:clj (str (java.util.UUID/randomUUID))
     :cljs (random-uuid)))

(comment
  (promote "IR")
  (promote "U")
  (demote "IR")
  (demote "U"))
