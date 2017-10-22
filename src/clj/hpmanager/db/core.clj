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
    [taoensso.timbre :as log]
    [crypto.password.pbkdf2 :as crypto]

    [hpmanager.model.users :as users])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Characters
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; User Management
(defn get-user-data
  "Returns userdata of a specific user, minus the password"
  [uname]
  (-> *db*
      (get-at! [::users uname])
      (dissoc ::users/password)))
(defn login-user
  "Return userdata iff the login is correct, else nil"
  [uname password]
  (if (and uname password) ; Return nil if not true
    (if-let [u (get-at! *db* [::users uname])]
      (do
        (log/infof "Attempting login request for user: %s with password: %s" uname password)
        (if (crypto/check password (::users/password u)) ; Return nil if not true
          (dissoc u ::users/password))))))
(defn create-user
  "Return the username iff we've created a new user, else nil"
  [uname password]
  (let [user (users/construct-user uname (crypto/encrypt password))
        ret (atom nil)]
    (with-write-transaction [*db* tx]
      (if-not (get-at tx [::users uname])
        (do
          (reset! ret uname)
          (assoc-at tx [::users uname] user))
        tx))
    @ret))
(comment
  (get-at! *db*)
  (dissoc-at! *db* ::users)
  (create-user "Admin" "TestPass")
  (create-user "Player1" "TestPass")
  (create-user "Player2" "TestPass")
  (create-user "Player3" "TestPass")
  (create-user "Player4" "TestPass")
  (create-user "Player5" "TestPass")
  (login-user "Admin" "TestPass")
  (login-user "Admin" "WrongPass")
  )



(comment
  (mount.core/start #'*db*)
  )
