(ns hpmanager.model.specialties
  (:require [clojure.spec.alpha :as s]
            [hpmanager.model.shared :as shared]
            )
  )

(s/def ::name (s/and string? (comp pos? count)))
;; Used when on minions, if they have a modifier
(s/def ::modifier (s/nilable (s/and integer? pos?)))
(s/def ::parent (s/nilable ::name))
(s/def ::description (s/and string? (comp pos? count)))
(s/def ::specialty (s/keys :req [::name]
                           :opt [::parent ::modifier ::description]))
(s/def ::specialties (s/map-of ::name ::specialty))
;; If this thing has specialties, it'll be under the key
(s/def ::module (s/keys :req [::specialties]))

(defn construct-specialty
  ([name]
   {::name name})
  ([name parent]
   {::name name
    ::parent parent})
  ([name parent description]
   {::name name
    ::parent parent
    ::description description})
  ([name parent description modifier]
   {::name name
    ::parent parent
    ::description description
    ::modifier modifier}))
(s/fdef construct-specialty
        :ret ::module)
(defn add-specialty-internal
  "Adds a specialty to a specialty map, _not_ to a module"
  [specialties {:as specialty name ::name}]
  (assoc specialties name specialty))
(defn add-specialty
  [module specialty]
  (update-in module [::specialties] add-specialty-internal specialty))
(s/fdef add-specialty
        :args (s/cat :module ::module
                     :specialty ::specialty)
        :ret ::module)
(defn get-specialty
  [module specialty-name]
  (get-in module [::specialties specialty-name]))
(s/fdef get-specialty
        :args (s/cat :module ::module
                     :specialty ::name)
        :ret ::module)

(def all-specialties
  "A list of all possible specialties"
  (let [construct-and-add (fn [m & s-args] (add-specialty-internal m (apply construct-specialty s-args)))]
    (-> {}
        ;; Management
        (construct-and-add "Assessment" "Management")
        (construct-and-add "Co-Ordination" "Management")
        (construct-and-add "Hygiene" "Management")
        (construct-and-add "Interrogation" "Management")
        (construct-and-add "Intimidation" "Management")
        (construct-and-add "Paperwork" "Management")
        (construct-and-add "Thought Control" "Management")
        (construct-and-add "Thought Survery" "Management")
        ;; Subterfuge
        (construct-and-add "Covert Operations" "Subterfuge")
        (construct-and-add "Infiltration" "Subterfuge")
        (construct-and-add "Investigation" "Subterfuge")
        (construct-and-add "Security Systems" "Subterfuge")
        (construct-and-add "Surveillance" "Subterfuge")
        (construct-and-add "Cleanup" "Subterfuge")
        (construct-and-add "Sabotage" "Subterfuge")
        (construct-and-add "Black Marketeering" "Subterfuge")
        ;; Violence
        (construct-and-add "Assault" "Violence")
        (construct-and-add "Command" "Violence")
        (construct-and-add "Crowd Control" "Violence")
        (construct-and-add "Demolition" "Violence")
        (construct-and-add "Outdoor Operations" "Violence")
        (construct-and-add "Defence" "Violence")
        (construct-and-add "Wetwork" "Violence")
        (construct-and-add "Total War" "Violence")
        ;; Hardware
        (construct-and-add "Bot Engineering" "Hardware")
        (construct-and-add "Construction" "Hardware")
        (construct-and-add "Chemical Engineering" "Hardware")
        (construct-and-add "Habitat Engineering" "Hardware")
        (construct-and-add "Nuclear Engineering" "Hardware")
        (construct-and-add "Production" "Hardware")
        (construct-and-add "Weird Science" "Hardware")
        (construct-and-add "Transport" "Hardware")
        ;; Software
        (construct-and-add "Bot Programming" "Software")
        (construct-and-add "Communications" "Software")
        (construct-and-add "Computer Security" "Software")
        (construct-and-add "Data Retrieval" "Software")
        (construct-and-add "Financial Systems" "Software")
        (construct-and-add "Hacking" "Software")
        (construct-and-add "Logistics" "Software")
        (construct-and-add "Media Manipulation" "Software")
        ;; Wetware
        (construct-and-add "Biosciences" "Wetware")
        (construct-and-add "Catering" "Wetware")
        (construct-and-add "Cloning" "Wetware")
        (construct-and-add "Medical" "Wetware")
        (construct-and-add "Mutant Studies" "Wetware")
        (construct-and-add "Outdoor Studies" "Wetware")
        (construct-and-add "Pharmatherapy" "Wetware")
        (construct-and-add "Subliminal Messaging" "Wetware")
        ;; Weird
        (construct-and-add "Bigger Guns" nil)
        (construct-and-add "Cyborging" nil)
        (construct-and-add "Propaganda" nil)
        (construct-and-add "Disruption" nil)
        (construct-and-add "Procurement" nil)
        (construct-and-add "Mystic Weirdness" nil)
        (construct-and-add "Gadgeteering" nil "Instead of activating this minion to make skill rolls, it can instead give another minion a new specialty (or increase an existing one by +4). This may have side effects.")
        (construct-and-add "Old Stuff" nil)
        (construct-and-add "Running" nil)
        (construct-and-add "Salvage" nil)
        (construct-and-add "Troubleshooting" nil)
        (construct-and-add "Must not Fail" nil "It is moderately treasonous to order this minion into a situation they get damaged or are humiliated.")
        (construct-and-add "Super Armoured" nil "This minion can only be damaged by Demolition or Total War")
        (construct-and-add "Middle Managers" nil "Can be attached to another minion to make the far, far less effective at their job.")
        (construct-and-add "Doubles Public Standing" nil "Any public standing gained or lost by this minion is doubled.")
        (construct-and-add "Fast Response" nil "This minion can be anywhere the first time they're bought.")
        (construct-and-add "Vault Diving" nil "Once per session, you can send this minion to go vault diving. They may bring back something juicy, or may not return at all.")
        (construct-and-add "One shot, Multiples" nil "Each time you buy this minion, you get a copy of it, and give that copy a single mission to complete. When it's finished it deactivates.")
        )))
