(ns pro.bohlender.mastermind-solver.app
  (:require [reagent.core :as r]
            [reagent.dom.client :refer [create-root]]
            [pro.bohlender.mastermind-solver.components :as components]))

(defonce root (->> "root"
                   (.getElementById js/document)
                   create-root))

(defn init []
    (js/console.log "app init called")
    (.render root (r/as-element [components/page])))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (init))
