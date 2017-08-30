(ns hpmanager.model.user
  "Deals with user accounts and session management"
  (:require [clojure.spec.alpha :as s]
            [hpmanager.db.core :as db]
            )
  )

(def ^:private sessions
  "Current active sessions, and their users"
  ;; TODO add validator
  (atom {}))
