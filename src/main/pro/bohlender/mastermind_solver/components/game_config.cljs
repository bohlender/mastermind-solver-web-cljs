(ns pro.bohlender.mastermind-solver.components.game-config
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [pro.bohlender.mastermind-solver.datastructures :refer [->Config]]))

(def game-configs
  {"Mastermind"          (->Config ["\uD83D\uDD35" "\uD83D\uDFE2" "\uD83D\uDFE1" "\uD83D\uDD34" "⚫" "⚪"]
                                   4)
   "Word Mastermind"     (->Config (vec (seq "abcdefghijklmnopqrstuvwxyz"))
                                   6)
   "Number Mastermind"   (->Config ["1" "2" "3" "4" "5" "6"]
                                   4)
   "Mastermind for Kids" (->Config ["\uD83D\uDC2F" "\uD83D\uDC12" "\uD83D\uDC18" "\uD83D\uDC3B" "\uD83E\uDD9B" "\uD83E\uDD81"]
                                   3)
   "Mansions of Madness" (->Config ["\uD83D\uDFE2" "\uD83D\uDFE1" "\uD83D\uDD35" "\uD83D\uDD34" "⚪" "\uD83D\uDFE3"]
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

(defn invalid-symbol-text? [symbol-text]
  (cond
    (nil? symbol-text) "Symbols are required"
    (empty? (re-seq #"\S+" symbol-text)) "At least one needed"))

(defn invalid-code-length-text? [code-length-text]
  (cond
    (nil? code-length-text) "Code length is required"
    (nil? (parse-long code-length-text)) "Not an integer"))

(defn allowed-symbols-field [value & {:keys [on-change disabled error] :or {disabled false}}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Allowed symbols"]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input {:type      "text"
                     :disabled  disabled
                     :value     value
                     :on-change on-change
                     :class     (when error "is-danger")}]]
     [:p.help.is-danger error]]]])

(defn code-length-field [value & {:keys [on-change disabled error] :or {disabled false}}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Code length"]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input {:type      "number"
                     :min       1
                     :disabled  disabled
                     :value     value
                     :on-change on-change
                     :class     (when error "is-danger")}]]
     [:p.help.is-danger error]]]])

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
         [:div.block {:hidden @custom?-atom}
          [allowed-symbols-field (string/join " " valid-symbols) :disabled true]
          [code-length-field code-length :disabled true]])
       ; Custom
       [:div.block {:hidden (not @custom?-atom)}
        [allowed-symbols-field (:value @custom-valid-symbols-atom)
         :on-change #(swap! custom-valid-symbols-atom assoc
                            :value (-> % .-target .-value))
         :error (:error @custom-valid-symbols-atom)]
        [code-length-field (:value @custom-code-length-atom)
         :on-change #(swap! custom-code-length-atom assoc
                            :value (-> % .-target .-value))
         :error (:error @custom-code-length-atom)]]
       [:div.block>div.buttons.is-right
        [:button.button.is-warning
         {:on-click (fn []
                      (if-not @custom?-atom
                        (on-submit-config (get game-configs @predefined-config-id-atom))
                        (do
                          (swap! custom-valid-symbols-atom assoc
                                 :error (invalid-symbol-text? (:value @custom-valid-symbols-atom)))
                          (swap! custom-code-length-atom assoc
                                 :error (invalid-code-length-text? (:value @custom-code-length-atom)))
                          (when-not (or (:error @custom-valid-symbols-atom)
                                        (:error @custom-code-length-atom))
                            (on-submit-config (->Config (re-seq #"\S+" (:value @custom-valid-symbols-atom))
                                                   (parse-long (:value @custom-code-length-atom))))))))}
         "Initialise new solver"]]])))
