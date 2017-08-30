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

(defn db-login-user
  "Gets a user from the database with an id/password pair.
  Returns the user if the password is correct, else returns nil"
  [id pass]
  (let [user (get-user {:id id})]
    (if (password/check pass (:pass user))
      (dissoc user :pass)
      nil)))

(defn db-create-user!
  "Creates a new user if not already existing.
  Handles password hashing.
  Returns the user (without the password field) if successful
  Throws an exception if the user already exists, or some other difficulty occurs."
  ;; TODO password complexity constraints
  [{:keys [id first_name last_name email pass] :as user}]
  (if-not pass
    (throw (Exception. "User requires a password"))
    (do
      (create-user! (assoc user :pass (password/encrypt pass)))
      (db-login-user id pass))))

(comment
         (db-create-user! {:id "testId" :first_name "testFirst" :last_name "testLast" :email "test@test.com" :pass "testPass"})
         (time (db-login-user "testId" "testPass"))
         (time (db-login-user "testId" "testPassBad"))
         )
