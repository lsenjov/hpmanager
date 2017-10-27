(ns hpmanager.model.minions
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            [hpmanager.model.specialties :as specialties]
            )
  )

(s/def ::name (s/and string? (comp pos? count)))
(s/def ::id (s/and string? (comp pos? count)))
(s/def ::owner (s/nilable (s/and string? (comp pos? count))))
;; Minions include secret societies, so it may not have a service group
(s/def ::service-group (s/nilable (s/and string? (comp pos? count))))
(s/def ::cost (s/nilable (s/and integer? (comp not neg?))))
;; Does purchasing this minion create a _new_ minion? Useful for troubleshooters/other one-use minions
(s/def ::multiples-buy? (s/nilable any?)) ; Truthy

(s/def ::minion (s/keys :req [::name ::id ::specialties/specialties]
                        :opt [::owner ::service-group ::notes
                              ::multiples-buy?]))
(s/def ::minions (s/map-of ::id ::minion))

(defn construct-minion
  [id minion-name
   {:as opts :keys [?multiples-buy? ?cost ?service-group]}]
  {::id id
   ::name minion-name
   ::cost ?cost
   ::service-group ?service-group
   ::multiples-buy? ?multiples-buy?})
(s/fdef construct-minion
        :ret ::minion)

(defn buy-minion
  [m owner]
