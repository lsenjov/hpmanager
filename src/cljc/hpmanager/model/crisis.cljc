(ns hpmanager.model.crisis
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            )
  )

(s/def ::crisis-announcement string?)
(s/def ::crisis-description (s/coll-of string?))
(s/def ::crisis (s/keys :req [::crisis-announcement ; What the GM may announce at the beginning of the day
                              ::crisis-description ; Extra background information for the GM
                              ;; TODO characters
                              ;; TODO societies
                              ;; TODO service groups/minions
                              ]))
