(ns pro.bohlender.mastermind-solver.solver.protocol)

(defprotocol Solver
  (load [this])
  (loaded? [this])
  (solve [this config history]))
