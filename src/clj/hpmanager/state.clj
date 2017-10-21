(ns hpmanager.state
  "This is the local storage for all state. If something on the server changes, it goes here."
  )

"Global state lives here"
(def global-state (atom {}))

;; TODO init global state
