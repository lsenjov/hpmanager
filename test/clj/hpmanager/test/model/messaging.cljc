(ns hpmanager.test.model.messaging
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [hpmanager.model.messaging :refer :all :as m])
  )

(deftest message-queue
  (testing "adding message"
    (let [message-one (construct-message
                        "user_a"
                        #{"user_a" "user_b"}
                        "Message content"
                        10000)
          message-two (construct-message
                        "user_b"
                        #{"user_a" "user_b"}
                        "Second message content"
                        11000)
          m-0 {::m/queue '()}]
      (is (s/valid? ::m/module m-0) "Constructed object is not a module, fix test.")
      (is (s/valid? ::m/module (add-message m-0 message-one)))
      (is (s/valid? ::m/module (-> m-0
                                   (add-message message-one)
                                   (add-message message-two)))))))

;; TODO testing to make sure we only get the latest messages when there's lots
