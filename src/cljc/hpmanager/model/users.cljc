(ns hpmanager.model.users
  "A namespace for tracking logged-in users"
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            )
  )

(s/def ::user-name
  (s/and string?
         #(pos? (count %))))
(s/def ::password
  (s/nilable (s/and string?
                    #(pos? (count %)))))
(s/def ::perms
  (s/nilable set?))
(s/def ::user
  (s/keys :req [::user-name ::password ::perms]))
(s/def ::users
  (s/map-of ::user-name ::user))
(s/def ::module
  (s/keys :req [::users]))

(defn construct-user
  ([user-name password perms]
   {::user-name user-name
    ::password password
    ::perms perms})
  ([user-name password]
   (construct-user user-name password #{}))
  ([user-name]
   (construct-user user-name nil)))
(s/fdef construct-user
        :args (s/or :3-args (s/cat :user-name ::user-name
                                   :password ::password
                                   :perms ::perms)
                    :2-args (s/cat :user-name ::user-name
                                   :password ::password)
                    :1-arg  (s/cat :user-name ::user-name)))

(defn get-user
  "Gets a user from the map, or nil if doesn't exist."
  [m user-name]
  (get-in m [::module ::users user-name]))
(defn login-user
  "Adds a user to the module, overwriting any supplied fields."
  [m {:as user ::keys [user-name]}]
  (update-in m [::module ::users user-name] merge user))
(defn logout-user
  "Removes a user from the module"
  [m user-name]
  (update-in m [::module ::users] #(dissoc % user-name)))
(defn strip-password
  [u]
  (dissoc u ::password))
