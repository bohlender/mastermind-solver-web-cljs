(ns pro.bohlender.mastermind-solver.solver
  (:require [pro.bohlender.mastermind-solver.solver.protocol :refer [Solver load loaded? solve]]
            [pro.bohlender.mastermind-solver.solver.simple :refer [->SimpleSolver]]
            [pro.bohlender.mastermind-solver.solver.sat :refer [->SatBasedSolver]]))


(defn- solve-with [solver]
  (fn [^js/MessageEvent event]
    (when-not (loaded? solver)
      (throw (js/Error. "Solver not loaded yet")))
    (let [data (js->clj (.-data event) :keywordize-keys true)]
      (let [res (solve solver (:config data) (:history data))]
        (js/postMessage (clj->js res))))))

;; Init

(defn init []
  (println "solver init called")
  (let [
        ;solver (->SimpleSolver)
        solver (->SatBasedSolver)
        ]
    (load solver)
    (js/addEventListener "message" (solve-with solver))
    (js/addEventListener "error" #(println "[Worker] Error: " %))))
