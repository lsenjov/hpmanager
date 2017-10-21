(ns hpmanager.model.messaging
  (:require [clojure.spec.alpha :as s]
            )
  )

(def global-chat-id "Global")

(s/def ::chat-id string?)
(s/def ::message-sender string?)
(s/def ::message-content string?)
(s/def ::message-recipients
  (s/or :everyone (partial = :any)
        :certain-people (s/and set?
                               (s/coll-of ::message-sender))))
(s/def ::all-message-recipients
  (s/and set?
         (s/coll-of ::message-recipients)))
;; TODO Represent times as longs
(s/def ::message-time-sent integer?)
(s/def ::message
  (s/keys :req [::chat-id ::message-sender ::message-content
                ::message-recipients ::message-time-sent]))

(s/def ::queue
  (s/and list?
         (s/coll-of ::message)))

(s/def ::chat ; So we can put more metadata on the chat later
  (s/keys :req [::queue]))

(s/def ::chats
  (s/map-of ::chat-id ::chat))

(s/def ::module
  (s/keys :req [::chats]))

(defn construct-message
  ([chat-id sender recipients content time-sent]
   {::chat-id chat-id
    ::message-sender sender
    ::message-recipients recipients
    ::message-content content
    ::message-time-sent time-sent})
  ([chat-id sender recipients content]
   {::chat-id chat-id
    ::message-sender sender
    ::message-recipients recipients
    ::message-content content}))
(s/fdef construct-message
        :args (s/cat ;; TODO update spec for 4 args
                :chat-id ::chat-id
                :sender ::message-sender
                :recipients ::message-recipients
                :content ::message-content
                :time-sent ::message-time-sent)
        :ret ::message)

(defn add-message
  [m {:as message ::keys [chat-id message-recipients]}]
  (-> m
      (update-in [::module ::chats chat-id ::queue] conj message)
      ;; Is a set, so we add the conversation to the chat
      (update-in [::module ::chats chat-id ::all-message-recipients]
                 (comp set #(conj % message-recipients))))) ; TODO remove set when we *know* it will be a set
(s/fdef add-message
        :args (s/cat :m ::module
                     :message ::message))

(defn get-categories
  [m chat-id]
  (get-in m [::module ::chats chat-id ::all-message-recipients]))
(s/fdef get-categories
        :args (s/cat :m ::module
                     :chat-id ::chat-id)
        :ret ::all-message-recipients)

(defn format-message
  "Formats a single message into a nicely readable string"
  [{:as message ::keys [message-sender message-content message-time-sent]}]
  (str \| "214-" (mod (quot message-time-sent 1000) 3153600) ; 3153600 seconds in a year
       \| \space message-sender ": " message-content))
(s/fdef format-message
        :args ::message)

(defn filter-messages
  "Filters the queue to only return messages meant for the recipient"
  ([q chat-id user n]
   (->> q
        (filter #((::message-recipients %) user))
        (take n)))
  ([q chat-id user]
   (filter-messages q user 100)))
(s/fdef filter-messages
        :args (s/or :2-args (s/cat :q ::queue
                                   :chat-id ::chat-id
                                   :user ::message-sender)
                    :3-args (s/cat :q ::queue
                                   :chat-id ::chat-id
                                   :user ::message-sender
                                   :n integer?))
        :ret ::queue)

(defn new-chat
  "Creates a new chat"
  [m chat-id]
  (add-message m
               (construct-message chat-id "System" :everyone "Chat Started" 0)))
(s/fdef new-chat
        :args (s/cat :m ::module
                     :chat-id ::chat-id))
(defn new-module
  "Creates a new chat module"
  []
  (new-chat {::module {}} global-chat-id))
(s/fdef new-module
        :ret ::module)

(defn filter-recipients
  "Filters the queue to only return messages for a select group of recipients"
  ([q rec]
   (filter q #(= rec (::message-recipients rec))))
  ([q rec n]
   (take n (filter-recipients q rec))))
(s/fdef filter-recipients
        :args ::queue
        :ret ::queue)
(defn get-messages
  ([m chat-id user-set n]
   (if (= :everyone user-set)
     (take n (get-in m [::module ::chats chat-id ::queue]))
     (take n (filter-recipients (get-in m [::module ::chats chat-id ::queue]) user-set))))
  ([m chat-id user-set]
   (get-messages m chat-id user-set 50)))

(comment
  (group-by first [[:a 1] [:b 2] [:b 3] [:c 4] [:a 5]])
  (distinct [2 3 4 5 5 4 3 2])
  (conj #{1 2 3} 4)
  )
