(ns pro.bohlender.mastermind-solver.components.history
  (:require [pro.bohlender.mastermind-solver.utils :refer [enumerate vec-remove-at]]
            [pro.bohlender.mastermind-solver.components.guess :refer [guess-component]]
            [pro.bohlender.mastermind-solver.datastructures :refer []]))

(defn- on-change-guess [history-atom entry-idx pos-idx new-value]
  (swap! history-atom assoc-in [entry-idx :guess pos-idx] new-value))

(defn- on-change-feedback [history-atom entry-idx feedback-kind new-value]
  (swap! history-atom assoc-in [entry-idx :feedback feedback-kind] new-value))

(defn- on-click-delete [history-atom entry-idx]
  (swap! history-atom vec-remove-at entry-idx))

(defn history-component [config-atom history-atom]
  [:div
   [:div.block
    [:p "Track the codemaker's feedback to your guesses here. Your next guess should be based on this information."]]
   [:div.block
    (if (empty? @history-atom)
      [:div.notification.is-warning.is-light
       "You haven't registered any guess yet."]
      [:div
       [:table.table.is-striped.is-fullwidth
        [:thead
         [:tr
          [:th.has-text-centered "Guess"]
          [:th.has-text-centered [:abbr {:title "How many symbols coincide with the secret"} "Full matches"]]
          [:th.has-text-centered [:abbr {:title "How many additional full matches can be achieved by reordering the symbols"} "Partial matches"]]
          [:th]]]
        [:tbody
         (doall
           (for [[idx entry] (enumerate @history-atom)]
             [:tr {:key idx}
              [:td.is-narrow [guess-component @config-atom (:guess entry) (partial on-change-guess history-atom idx)]]
              [:td [:input.input {:type      "number"
                                  :required  true
                                  :min       0
                                  :value     (get-in entry [:feedback :fm])
                                  :on-change #(on-change-feedback history-atom idx :fm (-> % .-target .-value int))}]]
              [:td [:input.input {:type      "number"
                                  :required  true
                                  :min       0
                                  :value     (get-in entry [:feedback :pm])
                                  :on-change #(on-change-feedback history-atom idx :pm (-> % .-target .-value int))}]]
              [:td.is-narrow [:button.button.is-danger
                              {:type     "button"
                               :on-click #(on-click-delete history-atom idx)}
                              [:span.icon [:i.fa.fa-trash]]]]]))]]])]])
