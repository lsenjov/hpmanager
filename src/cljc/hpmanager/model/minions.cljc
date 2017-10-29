(ns hpmanager.model.minions
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            [hpmanager.model.specialties :as specialties]
            )
  )

(s/def ::name (s/and string? (comp pos? count)))
(s/def ::id (s/and string? (comp pos? count)))
;; If owner is set, the minion has been bought
(s/def ::owner (s/nilable (s/and string? (comp pos? count))))
;; Minions include secret societies, so it may not have a service group
(s/def ::service-group (s/nilable (s/and string? (comp pos? count))))
(s/def ::cost (s/nilable (s/and integer? (comp not neg?))))
;; Does purchasing this minion create a _new_ minion? Useful for troubleshooters/other one-use minions
(s/def ::multiples-buy? (s/nilable any?)) ; Truthy

(s/def ::minion (s/keys :req [::name ::id ::specialties/module]
                        :opt [::owner ::service-group ::notes
                              ::multiples-buy?]))
(s/def ::minions (s/map-of ::id ::minion))

(defn construct-minion
  [id minion-name specialties
   {:as opts :keys [?multiples-buy? ?cost ?service-group]}]
  {::id id
   ::name minion-name
   ::specialties/module specialties
   ::cost ?cost
   ::service-group ?service-group
   ::multiples-buy? ?multiples-buy?})
(s/fdef construct-minion
        :args (s/cat :id ::id
                     :name ::name
                     :specialties ::specialties/module
                     :opts map?)
        :ret ::minion)

(defn buy-minion
  "Sets a minion to being bought."
  [ms minion-id new-owner]
  (if-let [{:as m ::keys [owner multiples-buy?]} (get ms minion-id)]
    (cond
      owner ; Already owned?
      ms

      multiples-buy? ; Create a new minion with a new uid and owned
      (let [new-uuid (shared/gen-uuid)]
        (assoc ms
               new-uuid
               (-> m
                   (assoc ::id new-uuid)
                   (assoc ::owner new-owner))))

      :else
      (assoc-in ms [minion-id ::owner] owner))
    ;; Minion doesn't exist, return the minions
    ms))
