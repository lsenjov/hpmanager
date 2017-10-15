(ns hpmanager.model.skills
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            )
  )

(def default-skills-map
  (->> ["Hardware" "Software" "Wetware" "Management" "Violence" "Subterfuge"]
       (map (fn [k] {k 1}))
       (apply merge {})))
(s/def ::skillmap
  (s/map-of string? (s/and integer? pos?))
  )

(defn construct-skills-map
  [sm]
  {::skills
   (merge default-skills-map sm)})

(defn get-clone-degredation
  [{sm ::skills}]
  (-> sm
      (get "Wetware")
      dec
      (quot 5)
      (->> (- 5))))
(defn get-society-count
  [{sm ::skills}]
  (-> sm
      (get "Management")
      dec
      (quot 5)
      (+ 2)))
(defn- lose-skill-point
  [{sm ::skills :as m}]
  (if-let [s (->> sm ; Find all skills with a value greater than 1 to decrement
                  (filter (fn [[_ v]] (< 1 v)))
                  ; Pick one at random
                  shuffle
                  first
                  first)]
    ; If we find at least one, decrement it
    (update-in m [::skills s] dec)
    ; Otherwise, ignore it
    sm))
(defn lose-skill-points
  [sm]
  ((apply comp (take (get-clone-degredation sm) (repeat lose-skill-point)))
   sm))
(def zap lose-skill-points)
(comment
  ;; TODO move to tests
  (def skills (construct-skills-map {"Wetware" 5 "Management" 10}))
  (lose-skill-points skills)
  (lose-skill-point {"Wetware" 5})
  (get-clone-degredation {"Wetware" 1})
  (get-clone-degredation {"Wetware" 5})
  (get-clone-degredation {"Wetware" 6})
  (get-clone-degredation {"Wetware" 15})
  (get-clone-degredation {"Wetware" 16})
  (get-clone-degredation {"Wetware" 20})
  (get-society-count {"Management" 1})
  (get-society-count {"Management" 5})
  (get-society-count {"Management" 6})
  (get-society-count {"Management" 15})
  (get-society-count {"Management" 16})
  (get-society-count {"Management" 20})
  )

