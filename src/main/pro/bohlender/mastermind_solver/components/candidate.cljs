(ns pro.bohlender.mastermind-solver.components.candidate
  (:require [reagent.core :as r]
            [cljs.core.async.interop :refer-macros [<p!]]
            [pro.bohlender.mastermind-solver.components.guess :refer [guess-component]]))

(defn- on-change-candidate [candidate-atom pos-idx new-value]
  (swap! candidate-atom assoc pos-idx new-value))

(defn candidate-component [candidate-atom config-atom history-atom]
  (let [loading? (r/atom false)
        ; TODO: Browser might not support workers
        worker (js/Worker. "/js/worker.js")]
    (.addEventListener worker "message" (fn [^js/MessageEvent event]
                                          ;(js/console.log event)
                                          (let [data (js->clj (.-data event))]
                                            (println data)
                                            (reset! candidate-atom data)
                                            (reset! loading? false))))
    (.addEventListener worker "error" #(println "[Worker] Error: " %))
    (fn []
      [:div
       ;[:h2.title.is-4 "Next guess"]
       [:table.table.is-striped.is-fullwidth
        [:thead
         [:tr
          [:th.has-text-centered {:colSpan 3} "Next guess"]]]
        [:tbody
         [:tr
          [:td.is-narrow
           [guess-component @config-atom @candidate-atom (partial on-change-candidate candidate-atom)]]
          [:td
           [:button.button.is-primary
            {:class    (when @loading? "is-loading")
             :on-click #(do
                          (reset! loading? true)
                          (.postMessage worker (clj->js {:config  @config-atom
                                                         :history @history-atom})))}
            [:span.icon [:i.fa.fa-lightbulb-o]]
            [:span "Get suggestion"]]]
          [:td.is-narrow
           [:button.button.is-success
            {:on-click #(swap! history-atom conj {:guess    @candidate-atom
                                                  :feedback {:fm nil, :pm nil}})}
            [:span.icon [:i.fa.fa-plus]]]]]]]])))
