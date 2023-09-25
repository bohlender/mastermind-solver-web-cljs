(ns pro.bohlender.mastermind-solver.components.candidate
  (:require [cljs.core.async.interop :refer-macros [<p!]]
            [pro.bohlender.mastermind-solver.components.guess :refer [guess-component]]))

(defn- on-change-candidate [candidate-atom pos-idx new-value]
  (swap! candidate-atom assoc pos-idx new-value))

(defn candidate-component [candidate-atom config-atom history-atom computing?-atom]
  (fn []
    [:div
     [:table.table.is-striped.is-fullwidth
      [:thead
       [:tr
        [:th.has-text-centered {:col-span 3} "Next guess"]]]
      [:tbody
       [:tr
        [:td.is-narrow
         [guess-component @config-atom @candidate-atom (partial on-change-candidate candidate-atom)]]
        [:td
         [:button.button.is-primary {:class (when @computing?-atom "is-loading")}
          [:span.icon [:i.fa.fa-lightbulb-o]]
          [:span "Get suggestion"]]]
        [:td.is-narrow
         [:button.button.is-success
          {:type     "button"
           :on-click #(swap! history-atom conj {:guess    @candidate-atom
                                                :feedback {:fm nil, :pm nil}})}
          [:span.icon [:i.fa.fa-plus]]]]]]]]))
