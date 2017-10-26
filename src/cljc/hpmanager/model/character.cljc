(ns hpmanager.model.character
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.names :as names]
            [hpmanager.model.skills :as skills]
            )
  )

(s/def ::hp-char
  (s/and ::names/named ::skills/skillmap))

(defn construct-character
  [char-name skill-map]
  (merge char-name skill-map))

(defn zap
  [c]
  (-> c
      names/zap
      skills/zap))

(comment
  (-> (construct-character (names/construct-name "Ann" "R" "KEY" 2)
                           (skills/construct-skills-map {"Wetware" 5 "Management" 8}))
      zap
      )
  )
