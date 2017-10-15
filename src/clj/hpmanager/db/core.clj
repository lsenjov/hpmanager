(ns hpmanager.db.core
  "The database does a couple of things:
  - Serves static data
  - Allows modification of static data
  - Syncs characters "
  (:require
    [clojure.spec.alpha :as s]
    [codax.core :refer :all]
    ;[clj-time.jdbc]
    ;[clojure.java.jdbc :as jdbc]
    ;[conman.core :as conman]
    [hpmanager.config :refer [env]]
    [mount.core :refer [defstate]]
    [crypto.password.bcrypt :as password])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Specs
;; If it exists in our database, it has a uuid
(s/def ::uuid (s/and string? #(= 36 (count %))))

(defstate ^:dynamic *db*
  :start (open-database (env :database-filepath))
  :stop (close-database *db*))

(defn- gen-uuid
  "Generates a random uuid"
  []
  (str (java.util.UUID/randomUUID)))

(defn- get-obj
  "Gets an object type by uuid"
  [t u]
  (get-at *db* [t u]))
(defn- set-obj!
  "Sets/overwrites an object type by uuid.
  If the object doesn't have a uuid, generates one"
  [t {uuid ::uuid :as o}]
  (if-not uuid
    (recur t (assoc o ::uuid (gen-uuid)))
    (assoc-at! *db* [t uuid] o)))

(defn get-character
  "Gets a character from the database by uuid"
  [uuid]
  (get-obj ::characters uuid))
(defn set-character!
  "Overwrites a character in the database, or sets if doesn't exist"
  [character]
  (set-obj! ::characters character))

(comment
  (set-character! {:name "testchar"})
  (assoc-at! *db* [:other-key] "other data")
  (get-at! *db*)
  (with-write-transaction [*db* tx]
    ((apply comp (map #(fn [t] (dissoc-at t %))
                     (map vector (keys (get-at tx)))))
     tx))
  (with-read-transaction [*db* tx]
    (-> tx
        (get-at [::characters])
        keys))
  )
