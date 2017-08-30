(ns hpmanager.model.messaging
  (:require [clojure.spec.alpha :as s]
            )
  )

(s/def ::message-sender string?)
(s/def ::message-content string?)
(s/def ::message-recipients
  (s/and set?
         (s/coll-of ::message-sender)))
;; TODO Represent times as longs
(s/def ::message-time-sent integer?)
(s/def ::message
  (s/keys :req [::message-sender ::message-content ::message-recipients ::message-time-sent]))

(s/def ::queue
  (s/and vector?
         (s/coll-of ::message)))

(s/def ::module
  (s/keys :req [::queue]))

(defn construct-message
  [sender recipients content time-sent]
  {::message-sender sender
   ::message-recipients recipients
   ::message-content content
   ::message-time-sent time-sent})
(s/fdef construct-message
        :args (s/cat :sender ::message-sender
                     :recipients ::message-recipients
                     :content ::message-content
                     :time-sent ::message-time-sent)
        :ret ::message)

(defn add-message
  [m message]
  (update-in m [::queue] conj message))
(s/fdef add-message
        :args (s/cat :m ::module
                     :message ::message))

(defn filter-messages
  "Filters the queue to only return messages meant for the recipient"
  ([m user n]
   (update-in m [::queue] (comp vector
                                (partial take n)
                                (partial filter #((::message-recipients %) user)))))
  ([m user]
   (filter-messages m user 100)))
(s/fdef filter-messages
        :args (s/or :2-args (s/cat :m ::module
                                   :user ::message-sender)
                    :3-args (s/cat :m ::module
                                   :user ::message-sender
                                   :n integer?))
        :ret ::module)
