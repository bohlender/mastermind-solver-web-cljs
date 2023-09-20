(ns pro.bohlender.mastermind-solver.components
  (:require [reagent.core :as r]
            [pro.bohlender.mastermind-solver.components.game-config :refer [config-component]]
            [pro.bohlender.mastermind-solver.components.history :refer [history-component]]
            [pro.bohlender.mastermind-solver.components.candidate :refer [candidate-component]]
            [pro.bohlender.mastermind-solver.components.tabs :refer [tabs-component]]
            [pro.bohlender.mastermind-solver.datastructures :refer [->Config ->Step ->Feedback]]))

(defn default-guess [config]
  (let [{:keys [valid-symbols code-length]} config]
    (->> valid-symbols first (repeat code-length) vec)))

(defn page []
  (let [config-atom (r/atom (->Config ["⚫" "\uD83D\uDD35" "\uD83D\uDFE2" "\uD83D\uDFE3" "\uD83D\uDD34" "\uD83D\uDFE1"] 4))
        history-atom (r/atom
                       [(->Step ["⚫" "\uD83D\uDFE3" "\uD83D\uDFE3" "\uD83D\uDD35"]
                                (->Feedback 0 1))
                        (->Step ["\uD83D\uDD34" "\uD83D\uDFE1" "⚫" "⚫"]
                                (->Feedback 1 2))
                        (->Step ["\uD83D\uDFE1" "\uD83D\uDFE2" "⚫" "\uD83D\uDD34"]
                                (->Feedback 2 1))])
        candidate-atom (r/atom (default-guess @config-atom))
        active-tab-idx-atom (r/atom 1)]
    (letfn [(init-solver [new-config]
              (reset! config-atom new-config)
              (reset! history-atom [])
              (reset! candidate-atom (default-guess @config-atom))
              (reset! active-tab-idx-atom 1))]
      (fn []
        [:div.section
         [:div.block
          [:h1.title "Mastermind Solver"]
          [:p
           "In " [:a {:href "https://en.wikipedia.org/wiki/Mastermind_(board_game)"} "Mastermind"] "-style puzzles you're expected to guess a secret code in as few tries as possible – only guided by the codemaker's feedback on your guesses."
           " For human players such puzzles can quickly grow intractable so this page implements " [:a {:href "https://bohlender.pro/blog/playing-hard-mastermind-games-with-a-sat-based-ai/"} "a decision procedure"] " to help with making sensible guesses."]]
         [:div.block
          [tabs-component
           {[:a
             [:span.icon [:i.fa.fa-cog]]
             [:span "Puzzle variant"]]
            [:div
             [config-component init-solver]]
            [:a
             [:span.icon [:i.fa.fa-lightbulb-o]]
             [:span "Solver"]]
            [:div
             [:div.block
              [history-component config-atom history-atom]]
             [:div.block
              [candidate-component candidate-atom config-atom history-atom]]]}
           active-tab-idx-atom]]]))))
