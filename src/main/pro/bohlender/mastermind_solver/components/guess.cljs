(ns pro.bohlender.mastermind-solver.components.guess
  (:require [pro.bohlender.mastermind-solver.utils :refer [enumerate]]))

(defn guess-component [config guess on-nth-change]
  (let [valid-symbols (:valid-symbols config)
        options (for [[idx sym] (enumerate valid-symbols)]
                  ^{:key idx} [:option sym])]
    [:div
     (for [[idx sym] (enumerate guess)]
       ^{:key idx} [:div.select
                    [:select
                     {:value sym
                      :on-change #(on-nth-change idx (-> % .-target .-value))}
                     options]])]))
