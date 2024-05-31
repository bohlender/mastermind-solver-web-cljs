(ns pro.bohlender.mastermind-solver.solver.sat
  (:require [pro.bohlender.mastermind-solver.solver.protocol :refer [Solver]]
            [wasm-ffi :refer [Wrapper Struct Pointer types]]
            [clojure.set :refer [map-invert]]))

(def WasmGameConfig (Struct. (clj->js {:numSymbols   "int"
                                       :secretLength "int"})))

(def WasmFeedback (Struct. (clj->js {:fullMatches    "int"
                                     :partialMatches "int"})))

(def WasmGuess (Struct. (clj->js {:elements (types.pointer "int") ; array
                                  :length   "int"})))

(def WasmGameHistoryEntry (Struct. (clj->js {:guess    (types.pointer WasmGuess)
                                             :feedback (types.pointer WasmFeedback)})))

(def WasmGameHistory (Struct. (clj->js {:elements (types.pointer (types.pointer WasmGameHistoryEntry)) ; array
                                        :length   "int"})))

(def library
  (Wrapper. (clj->js {:mkGameConfig       [WasmGameConfig, ["number", "number"]]
                      ;:mkFeedback         [WasmFeedback, ["number", "number"]]
                      :mkGuess            [WasmGuess, ["number", "number"]]
                      ;:dbgFeedback        [WasmFeedback, ["number", "number"]]
                      ;:dbgSolve           ["void", ["number", "number"]]
                      :mkGameHistoryEntry [WasmGameHistoryEntry, [WasmGuess, WasmFeedback]]
                      :mkGameHistory      [WasmGameHistory, ["number", "number"]]
                      :solve              ["void", [WasmGameConfig, WasmGameHistory, WasmGuess]] ; last arg is res
                      })))

(defn Config->WasmGameConfig [{:keys [valid-symbols code-length] :as config}]
  (.mkGameConfig library (count valid-symbols) code-length))

(defn Feedback->WasmFeedback [feedback]
  (let [{:keys [fm pm]} feedback
        ;res (.mkFeedback library fm pm)
        res (WasmFeedback. (clj->js {:fullMatches fm :partialMatches pm}))
        ]
    res))


(defn Guess->WasmGuess [valid-symbols guess]
  (let [decode-symbol-fn (->> (map-indexed vector valid-symbols)
                              (into {}))
        encode-symbol-fn (map-invert decode-symbol-fn)
        encoded-guess (map encode-symbol-fn guess)
        elements-addr (-> encoded-guess
                          clj->js
                          js/Int32Array.
                          library.utils.writeArray)]
    ;(println "guess elements addr: " elements-addr)
    ;(println "guess elements: " (js/Int32Array. library.exports.memory.buffer elements-addr (count guess)))
    (.mkGuess library elements-addr (count guess))))

(defn WasmGuess->Guess [{:keys [valid-symbols code-length] :as config} guess]
  (let [decode-symbol-fn (->> (map-indexed vector valid-symbols)
                              (into {}))]
    (->> (js/Int32Array. library.exports.memory.buffer (.ref (.-elements guess)) code-length)
         (mapv decode-symbol-fn))))


(defn GameHistory->WasmGameHistory [valid-symbols history]
  (let [elements (->> history
                      (map (fn [e] (.mkGameHistoryEntry library
                                                        (Guess->WasmGuess valid-symbols (:guess e))
                                                        (Feedback->WasmFeedback (:feedback e))))))
        addresses (map #(.ref %) elements)
        elements-addr (->> addresses
                           clj->js
                           js/Int32Array.
                           library.utils.writeArray)
        ;first-element-addr (.ref (first elements))
        ]
    ;(println "elements: " elements)
    ;(println "element-addrs: " addresses)
    ;(println "elements-addr: " elements-addr)
    ;(println "first-element-addr: " first-element-addr)
    ;(println "expected second-element-addr: " (.ref (second elements)))
    ;(println "actual element-addrs: " (js/Int32Array. library.exports.memory.buffer elements-addr (count history)))
    (.mkGameHistory library elements-addr (count history))
    ))


(def loaded?-atom (atom false))

; TODO: Consider passing after-init callback
(defn load-wasm []
  (-> (.fetch library "/js/function.wasm")
      (.then (fn [_] (reset! loaded?-atom true)))))


(defn- solve [{:keys [valid-symbols code-length] :as config} history]
  ; encode GameConfig -> WasmGameConfig in mem
  ; encode GameHistory -> WasmGameHistory in mem
  ; Pass input & allocated mem for output to WASM solver
  ; interpret WasmGuess in mem -> Guess
  ; dealloc WasmGuess, WasmGameConfig, WasmGameHistory

  (let [wasm-config (Config->WasmGameConfig config)
        wasm-history (GameHistory->WasmGameHistory valid-symbols history)
        wasm-best-guess (.mkGuess library (library.utils.writeArray (js/Int32Array. code-length)) code-length)
        ]
    ;(println history)
    ;(println "history: " wasm-history)
    ;(let [elements-addr (.ref (.-elements wasm-history))    ; works with e
    ;      element-addrs (js/Int32Array. library.exports.memory.buffer elements-addr (.-length wasm-history))]
    ;  (println "elements-addr: " elements-addr)
    ;  (println "element-addrs: " element-addrs)
    ;  (doseq [element-addr element-addrs]
    ;    (let [element (library.utils.readStruct element-addr WasmGameHistoryEntry)]
    ;      (println element))))


    (.solve library wasm-config wasm-history wasm-best-guess)
    (let [res (WasmGuess->Guess config wasm-best-guess)]
      (println "best guess:" res)
      res)))

(defrecord SatBasedSolver []
  Solver
  (load [_] (load-wasm))
  (loaded? [_] @loaded?-atom)
  (solve [_ config history] (solve config history)))
