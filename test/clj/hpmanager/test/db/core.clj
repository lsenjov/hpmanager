(ns hpmanager.test.db.core
  (:require [hpmanager.db.core :refer [*db*] :as db]
            ;[luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            ;[clojure.java.jdbc :as jdbc]
            [codax.core :refer :all]
            [hpmanager.config :refer [env]]
            [mount.core :as mount]))

(defn- clear-database
  "Clears the database completely."
  []
  (with-write-transaction [db/*db* tx]
    ((apply comp (map #(fn [t] (dissoc-at t %))
                      (map vector (keys (get-at tx)))))
     tx)))

;; Mounts the database
(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'hpmanager.config/env
      #'hpmanager.db.core/*db*)
    (f)))
;; Clears the database before each test
(use-fixtures
  :each
  (fn [f]
    (clear-database)
    (f)))

(deftest test-character-store
  (let [c (db/set-character! {:name "TestName"})]
    (is (= "TestName" (:name c)) "Should still have the same name")
    (is (::db/uuid c) "Should have a uuid set")))
