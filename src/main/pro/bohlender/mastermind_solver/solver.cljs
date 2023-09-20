(ns pro.bohlender.mastermind-solver.solver
  (:require [clojure.set :as set]
            [cljs.test :refer-macros [deftest is]]
            [cljs.math :refer [pow floor]]))


(defn eval-guess [guess secret]
  (let [; remove full matches from guess & secret
        [guess-rest secret-rest] (->> (map vector guess secret)
                                      (reduce (fn [[g s] [lhs rhs]]
                                                (if (= lhs rhs)
                                                  [g s]
                                                  [(conj g lhs) (conj s rhs)]))
                                              [[] []]))
        full-match-count (- (count guess) (count guess-rest))
        ; partial matches
        shared-symbols (set/intersection (set secret-rest) (set guess-rest))
        partial-matches (merge-with min
                                    (-> (frequencies guess-rest) (select-keys shared-symbols))
                                    (-> (frequencies secret-rest) (select-keys shared-symbols)))
        partial-match-count (reduce + (vals partial-matches))]
    {:fm full-match-count
     :pm partial-match-count}))

(deftest eval-guess-test
  (let [secret ["green" "black" "black" "red"]]
    (is (= {:fm 0 :pm 1} (eval-guess ["black" "purple" "purple" "blue"] secret)))
    (is (= {:fm 1 :pm 2} (eval-guess ["red" "yellow" "black" "black"] secret)))
    (is (= {:fm 2 :pm 1} (eval-guess ["yellow" "green" "black" "red"] secret)))
    (is (= {:fm 4 :pm 0} (eval-guess ["green" "black" "black" "red"] secret)))))


(defn consistent? [secret guess feedback]
  (= (eval-guess guess secret) feedback))

(defn consistent-with-history? [secret history]
  (->> history
       (every? (fn [{:keys [guess feedback]}] (consistent? secret guess feedback)))))

(deftest consistent-with-history?-test
  (let [secret ["green" "black" "black" "red"]
        bad-guess ["red" "yellow" "black" "black"]
        history [{:guess    ["black" "purple" "purple" "blue"]
                  :feedback {:fm 0 :pm 1}}
                 {:guess    ["red" "yellow" "black" "black"]
                  :feedback {:fm 1 :pm 2}}
                 {:guess    ["yellow" "green" "black" "red"]
                  :feedback {:fm 2 :pm 1}}
                 {:guess    ["green" "black" "black" "red"]
                  :feedback {:fm 4 :pm 0}}]]
    (is (consistent-with-history? secret history))
    (is (not (consistent-with-history? bad-guess history)))))


(defn num-secrets [config]
  (let [num-symbols (count (:valid-symbols config))]
    (pow num-symbols (:code-length config))))

(deftest num-secrets-test
  (let [config {:valid-symbols ["black" "blue" "green" "purple" "red" "yellow"]
                :code-length   4}]
    (is (= 1296 (num-secrets config)))))


; https://github.com/clojure/math.combinatorics/blob/master/src/main/clojure/clojure/math/combinatorics.cljc
(defn cartesian-product
  "All the ways to take one item from each sequence"
  [& seqs]
  (let [v-original-seqs (vec seqs)
        step
        (fn step [v-seqs]
          (let [increment
                (fn [v-seqs]
                  (loop [i (dec (count v-seqs)), v-seqs v-seqs]
                    (if (= i -1) nil
                                 (if-let [rst (next (v-seqs i))]
                                   (assoc v-seqs i rst)
                                   (recur (dec i) (assoc v-seqs i (v-original-seqs i)))))))]
            (when v-seqs
              (cons (map first v-seqs)
                    (lazy-seq (step (increment v-seqs)))))))]
    (when (every? seq seqs)
      (lazy-seq (step v-original-seqs)))))

(defn all-secrets [config]
  (->> (:valid-symbols config)
       (repeat (:code-length config))
       (apply cartesian-product)))

(deftest all-secrets-test
  (let [config {:valid-symbols ["black" "blue" "green" "purple" "red" "yellow"]
                :code-length   4}]
    (is (= 1296 (->> (all-secrets config)
                     set
                     count)))))


(defn nth-secret [config n]
  (let [{:keys [valid-symbols code-length]} config
        num-symbols (->> valid-symbols set count)]
    (->> (loop [n n
                i (dec code-length)
                retval []]
           (if (neg? i)
             retval
             (let [factor (pow num-symbols i)
                   times (floor (unchecked-divide-int n factor))]
               (recur (- n (* factor times))
                      (dec i)
                      (conj retval times)))))
         (mapv #(nth valid-symbols %)))))

(deftest nth-secret-test
  (let [config {:valid-symbols ["black" "blue" "green" "purple" "red" "yellow"]
                :code-length   4}]
    (is (= (nth-secret config 0) ["black" "black" "black" "black"]))
    (is (= (nth-secret config 8) ["black" "black" "blue" "green"]))
    (is (= (nth-secret config 1295) ["yellow" "yellow" "yellow" "yellow"]))))


(defn solve [config history]
  (println "Solving for:" config)
  (doseq [entry history]
    (println entry))
  ; random
  ;(loop [n-seq (repeatedly #(rand-int (num-secrets config)))]
  ;  (let [guess (nth-secret config (first n-seq))]
  ;    (if (consistent-with-history? guess history)
  ;      guess
  ;      (recur (rest n-seq)))))
  ; sequential
  (->> (all-secrets config)
       (filter #(consistent-with-history? % history))
       first)
  ; sequential 2
  ;(->> (for [n (range (dec (num-secrets config)))]
  ;       (nth-secret config n))
  ;     (filter #(consistent-with-history? % history))
  ;     first)
  )

(deftest solve-test
  (let [config {:valid-symbols ["black" "blue" "green" "purple" "red" "yellow"]
                :code-length   4}
        history [{:guess    ["black" "purple" "purple" "blue"]
                  :feedback {:fm 0 :pm 1}}
                 {:guess    ["red" "yellow" "black" "black"]
                  :feedback {:fm 1 :pm 2}}
                 {:guess    ["yellow" "green" "black" "red"]
                  :feedback {:fm 2 :pm 1}}
                 {:guess    ["green" "black" "black" "red"]
                  :feedback {:fm 4 :pm 0}}]]
    (is (= ["green" "black" "black" "red"] (solve config history))))
  (let [config {:valid-symbols [:a :b :c]
                :code-length   3}
        history [{:guess    [:a :a :a]
                  :feedback {:fm 1 :pm 1}}]]
    (is (= nil (solve config history)))))


(defn- fake-solve [^js/MessageEvent event]
  (let [data (js->clj (.-data event) :keywordize-keys true)]
    (let [res (solve (:config data) (:history data))]
      (js/postMessage (clj->js res)))))

(defn init []
  (println "solver init called")
  (js/addEventListener "message" fake-solve)
  (js/addEventListener "error" #(println "[Worker] Error: " %)))
