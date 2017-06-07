(ns hpmanager.db.core
  (:require
    [clj-time.jdbc]
    [clojure.java.jdbc :as jdbc]
    [conman.core :as conman]
    [hpmanager.config :refer [env]]
    [mount.core :refer [defstate]]
    [crypto.password.bcrypt :as password])
  (:import [java.sql
            BatchUpdateException
            PreparedStatement]))

(defstate ^:dynamic *db*
           :start (conman/connect! {:jdbc-url (env :database-url)})
           :stop (conman/disconnect! *db*))

;; This takes the queries and binds them to private functions
(conman/bind-connection *db* "sql/queries.sql")

(defn db-create-user!
  "Creates a new user if not already existing.
  Handles password hashing.
  Throws an exception if the user already exists"
  [id first-name last-name email pass]
  (create-user! {:id id
                 :first_name first-name
                 :last_name last-name
                 :email email
                 :pass (password/encrypt pass)}))

(defn db-login-user
  "Gets a user from the database with an id/password pair.
  Returns the user if the password is correct, else returns nil"
  [id pass]
  (let [user (get-user {:id id})]
    (if (password/check pass (:pass user))
      (dissoc user :pass)
      nil)))

(comment
         (db-create-user! "testId" "testFirst" "testLast" "test@test.com" "testPass")
         (db-login-user "testId" "testPass")
         (db-login-user "testId" "testPassBad")
         )
