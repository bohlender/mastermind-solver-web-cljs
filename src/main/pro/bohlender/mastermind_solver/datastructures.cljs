(ns pro.bohlender.mastermind-solver.datastructures)

(defrecord Config [valid-symbols code-length])

(defn mk-config [valid-symbols code-length]
  (->Config (set valid-symbols) code-length))

(defrecord Step [guess feedback])

(defrecord Feedback [fm pm])
