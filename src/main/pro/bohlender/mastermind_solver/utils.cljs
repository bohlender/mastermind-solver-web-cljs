(ns pro.bohlender.mastermind-solver.utils)

(defn enumerate [seq]
  (map-indexed vector seq))

(defn insert-between [[x & xs :as coll] y]
  (if xs
    (cons x (cons y (insert-between xs y)))
    coll))

(defn vec-remove-at [coll pos]
  (into (subvec coll 0 pos)
        (subvec coll (inc pos))))
