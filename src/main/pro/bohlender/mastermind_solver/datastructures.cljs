(ns pro.bohlender.mastermind-solver.datastructures)

(defrecord Config [valid-symbols code-length])

(defrecord Step [guess feedback])

(defrecord Feedback [fm pm])

