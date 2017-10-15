(ns hpmanager.model.names
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            )
  )

;; TODO figure whether to put this under a ::module subheader for easy synchronising later on
;; Choose early!
(s/def ::first-name (s/and string? #(pos? (count %))))
(s/def ::clearance (-> shared/clearance-to-rank keys set))
(s/def ::zone-name (s/and string? #(= 3 (count %))))
(s/def ::clone-num (s/and integer? pos?))
(s/def ::named (s/keys :req [::first-name ::clearance ::zone-name ::clone-num]))
(defn construct-name
  [first-name clearance zone-name clone-num]
  {::first-name first-name
   ::clearance clearance
   ::zone-name zone-name
   ::clone-num clone-num})
(s/fdef construct-name
        :args (s/cat :first-name ::first-name
                     :clearance ::clearance
                     :zone-name ::zone-name
                     :clone-num ::clone-num)
        :ret ::named)

(defn get-name
  [{::keys [first-name clearance zone-name clone-num]}]
  (str first-name \- clearance \- zone-name \- clone-num))
(defn zap
  [n]
  (update-in n [::clone-num] inc))

(comment
  (get-name (construct-name "Ann" "R" "KEY" 2))
  (get-name (zap (construct-name "Ann" "R" "KEY" 2)))
  )
