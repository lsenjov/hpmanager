(ns hpmanager.model.messaging
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            )
  )

(def global-chat-channel "global")

(s/def ::chat-id string?)
(s/def ::message-sender string?)
(s/def ::message-content string?)
(s/def ::message-recipients
  (s/or :everyone (partial = :everyone)
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
  ; In this case, ::message-recipients is everyone subscribed to the channel
  (s/keys :req [::queue ::message-recipients]))

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

(->> #{1 2 3 4} (map str) sort (interpose ", ") doall)
(defn add-message
  [m {:as message ::keys [chat-id message-recipients]}]
  (log/tracef "add-message. message: %s" message)
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
  ([q user n]
   (log/infof "q is: %s user is: %s" q user)
   (->> q
        (filter #(or
                   (= :everyone (::message-recipients %))
                   ((::message-recipients %) user)
                   ))
        (take n)))
  ([q user]
   (filter-messages q user 100)))
(s/fdef filter-messages
        :args (s/or :2-args (s/cat :q ::queue
                                   :user ::message-sender)
                    :3-args (s/cat :q ::queue
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
  ([m]
   (new-chat m global-chat-channel))
  ([]
   (new-module {::module {}})))
(s/fdef new-module
        :ret ::module)

(defn filter-recipients
  "Filters the queue to only return messages for a select group of recipients"
  ([q rec]
   (filter #(= rec (::message-recipients %)) q))
  ([q rec n]
   (take n (filter-recipients q rec))))
(s/fdef filter-recipients
        :args ::queue
        :ret ::queue)

(defn get-messages
  ([m chat-id user-set n]
   ;(if (= :everyone user-set)
   ;(take n (get-in m [::module ::chats chat-id ::queue]))
   (take n (filter-recipients (get-in m [::module ::chats chat-id ::queue]) user-set))
   ;)
   )
  ([m chat-id user-set]
   (get-messages m chat-id user-set 50)))
(s/fdef get-messages
        :args (s/cat :m ::module
                     :chat-id ::chat-id
                     :user-set ::message-recipients
                     :n (s/and number? pos?))
        :ret ::queue)

(defn sort-chat
  "Sorts a single chat in order, removes duplicates"
  ([m chat-id]
   (update-in m [::module ::chats chat-id ::queue]
              (comp (partial sort (comp > ::message-time-sent))
                    distinct))))
(s/fdef sort-chat
        :args (s/cat :m ::module
                     :chat-id ::chat-id)
        :ret ::module)
(defn add-messages
  "Takes a queue of messages meant for a single chat and adds them."
  [m chat-id q]
  (-> m
      (update-in [::module ::chats chat-id ::queue] concat q)
      (sort-chat chat-id)
      (update-in [::module ::chats chat-id ::all-message-recipients]
                 (comp set #(apply conj % (->> q ::message-recipients distinct))))))
(s/fdef add-messages
        :args (s/cat :m ::module
                     :chat-id ::chat-id
                     :q ::queue)
        :ret ::module)
(defn refresh-messages
  "Takes a queue of messages, adds them to the correct chat, sorts and ensures they're in order.
  Can take a queue containing messages for multiple chats"
  ([m q]
   (let [qs (group-by ::chat-id q)
         add-fn (apply comp
                       (map (fn [[chat-id queue]] ; Returns a function that takes a module and adds to the queue
                              (fn [module] (add-messages module chat-id queue)))
                            qs))]
     (add-fn m))))
(s/fdef refresh-messages
        :args (s/cat :m ::module
                     :q ::queue)
        :ret ::module)
(defn refresh-chat
  "Takes a chat with a chat-id, adds it to the module"
  ([m [chat-id {:as chat ::keys [queue message-recipients]}]]
   (-> m
       (refresh-messages queue)
       (assoc-in [::module ::chats chat-id ::message-recipients] message-recipients)
       )))
(defn get-messages-for-refresh
  "Takes a module and a user-id. Returns a map of chat-id to chat"
  [m uid]
  (->> (get-in m [::module ::chats])
       (filter (fn [[k {::keys [message-recipients]}]]
                 (or (= global-chat-channel k)
                     (message-recipients uid))))
       (map (fn [[k c]]
              {k (update-in c [::queue] filter-messages uid
                            )}))
       (apply merge {})))

(defn user-join
  "Adds a user to a channel"
  [m chat-id user]
  (update-in m [::module ::chats chat-id ::message-recipients]
             (comp set #(conj % user))))
(defn user-leave
  "Removes a user from a channel"
  [m chat-id user]
  (update-in m [::module ::chats chat-id ::message-recipients]
             (comp set #(disj % user))))
(defn get-users
  "Gets all users currently in the channel"
  [m chat-id]
  (get-in m [::module ::chats chat-id ::message-recipients]))

#?(:clj
   (defn add-time
     "Server side only. Adds the current time to a message"
     [m]
     (assoc m ::message-time-sent
            (.getTimeInMillis (java.util.Calendar/getInstance)))))


(comment
  (group-by first [[:a 1] [:b 2] [:b 3] [:c 4] [:a 5]])
  (distinct [2 3 4 5 5 4 3 2])
  (conj #{1 2 3} 4)
  )
