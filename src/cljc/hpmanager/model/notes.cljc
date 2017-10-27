(ns hpmanager.model.notes
  "Generic note-taking for objects"
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            [taoensso.timbre :as log]
            ))

(s/def ::content string?)
(s/def ::owner (s/and string? (comp pos? count)))
(s/def ::note (s/keys :req [::content ::owner]))
(s/def ::notes (s/map-of ::owner ::note))
