(ns pro.bohlender.mastermind-solver.components.tabs
  (:require [pro.bohlender.mastermind-solver.utils :refer [enumerate]]))

(defn tabs-component [components-map active-idx-atom]
  (fn []
    [:div
     [:div.tabs.is-centered.is-boxed
      [:ul
       (doall (for [[idx key] (-> components-map keys enumerate)]
                [:li
                 {:key      idx
                  :class    (when (= @active-idx-atom idx) "is-active")
                  :on-click #(reset! active-idx-atom idx)}
                 key]))]]
     [:div
      (doall (for [[idx component] (-> components-map vals enumerate)]
               [:div {:key idx, :hidden (not= @active-idx-atom idx)}
                component]))]]))
