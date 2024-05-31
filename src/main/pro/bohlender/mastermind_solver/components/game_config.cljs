(ns pro.bohlender.mastermind-solver.components.game-config
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [pro.bohlender.mastermind-solver.datastructures :refer [mk-config]]))

(def game-configs
  {"Mastermind"          (mk-config ["\uD83D\uDD35" "\uD83D\uDFE2" "\uD83D\uDFE1" "\uD83D\uDD34" "⚫" "⚪"]
                                   4)
   "Word Mastermind"     (mk-config (vec (seq "abcdefghijklmnopqrstuvwxyz"))
                                   6)
   "Number Mastermind"   (mk-config ["1" "2" "3" "4" "5" "6"]
                                   4)
   "Mastermind for Kids" (mk-config ["\uD83D\uDC2F" "\uD83D\uDC12" "\uD83D\uDC18" "\uD83D\uDC3B" "\uD83E\uDD9B" "\uD83E\uDD81"]
                                   3)
   "Mansions of Madness" (mk-config ["\uD83D\uDFE2" "\uD83D\uDFE1" "\uD83D\uDD35" "\uD83D\uDD34" "⚪" "\uD83D\uDFE3"]
                                   6)})

(defn config-chooser-component [game-config-id custom?]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Variant"]]
   [:div.field-body
    [:div.field
     [:div.level.is-mobile
      [:div.level-left
       [:div.level-item
        [:div.select
         [:select
          {:value     @game-config-id
           :on-change #(reset! game-config-id (-> % .-target .-value))
           :disabled  @custom?}
          (for [name (keys game-configs)]
            ^{:key name} [:option name])]]]
       [:div.level-item
        [:label.checkbox
         [:input.mr-1
          {:type      "checkbox"
           :checked   @custom?
           :on-change #(swap! custom? not)}]
         "Custom"]]]]]]])

(defn allowed-symbols-field [value & {:keys [on-change disabled] :or {disabled false}}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Allowed symbols"]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input {:type      "text"
                     :required  true
                     :disabled  disabled
                     :value     value
                     :pattern   ".*\\S.*"
                     :title     "needs one non-space character"
                     :on-change (do on-change)}]]]]])

(defn code-length-field [value & {:keys [on-change disabled] :or {disabled false}}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Code length"]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input {:type      "number"
                     :required  true
                     :min       1
                     :disabled  disabled
                     :value     value
                     :on-change on-change}]]]]])

(defn new-solver-button []
  [:div.block>div.buttons.is-right
   [:button.button.is-warning
    "Initialise new solver"]])

(defn config-component [on-submit-config]
  (let [custom?-atom (r/atom false)
        predefined-config-id-atom (r/atom (-> game-configs keys first))
        custom-valid-symbols-atom (r/atom {:value "" :error nil})
        custom-code-length-atom (r/atom {:value "" :error nil})]
    (fn []
      [:div
       [:div.block
        [:p "What characterises a puzzle variant is the set of allowed symbols and the length of the secret."]]
       [:div.block
        [config-chooser-component predefined-config-id-atom custom?-atom]]
       ; Predefined
       (let [{:keys [valid-symbols code-length]} (get game-configs @predefined-config-id-atom)]
         [:form.block
          {:hidden    @custom?-atom
           :on-submit (fn [^js/SubmitEvent e]
                        (.preventDefault e)
                        (on-submit-config (get game-configs @predefined-config-id-atom)))}
          [allowed-symbols-field (string/join " " valid-symbols) :disabled true]
          [code-length-field code-length :disabled true]
          [new-solver-button]])
       ; Custom
       [:form.block
        {:hidden    (not @custom?-atom)
         :on-submit (fn [^js/SubmitEvent e]
                      (.preventDefault e)
                      (on-submit-config (mk-config (re-seq #"\S+" (:value @custom-valid-symbols-atom))
                                                  (parse-long (:value @custom-code-length-atom)))))}
        [allowed-symbols-field (:value @custom-valid-symbols-atom)
         :on-change #(swap! custom-valid-symbols-atom assoc
                            :value (-> % .-target .-value))]
        [code-length-field (:value @custom-code-length-atom)
         :on-change #(swap! custom-code-length-atom assoc
                            :value (-> % .-target .-value))]
        [new-solver-button]]])))
