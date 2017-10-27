(ns hpmanager.model.specialties
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            )
  )

(s/def ::name (s/and string? (comp pos? count)))
(s/def ::value (s/and integer? pos?))
(s/def ::parent (s/nilable ::name))
(s/def ::specialty (s/keys :req [::name ::value]
                           :opt [::parent]))
(s/def ::specialties (s/map-of ::name ::specialty))
