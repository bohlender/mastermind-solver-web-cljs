(ns pro.bohlender.mastermind-solver.solver.sat
  (:require [pro.bohlender.mastermind-solver.solver.protocol :refer [Solver]]
            [clojure.set :refer [map-invert]]))

(def wasm-atom (atom nil))

(defn load-wasm []
  (-> (js/fetch "/js/mastermind_wasm.wasm")
      (js/WebAssembly.instantiateStreaming)
      (.then (fn [res]
               (reset! wasm-atom (.-instance res))
               ;(._initialize (.-exports (.-instance res))) // TODO: What for?
               ))))

(defn- solve [{:keys [valid-symbols code-length] :as config} history]
  (let [int->symbol (->> (map-indexed vector valid-symbols)
                         (into {}))
        symbol->int (map-invert int->symbol)
        decode-code-fn (fn [wasm-code] (map int->symbol wasm-code))
        encode-code-fn (fn [guess] (map symbol->int guess))
        exports (.-exports @wasm-atom)
        buffer (.-memory.buffer exports)]
    (letfn [(solver-ctor [num-symbols secret-length]
              (.Solver_ctor exports num-symbols secret-length))
            (solver-dtor [solver-ptr]
              (.Solver_dtor exports solver-ptr))
            (add-interaction [solver-ptr arr-ptr fm pm]
              (.add_interaction exports solver-ptr arr-ptr fm pm))
            (solve [solver-ptr code-ptr]
              (.solve exports solver-ptr code-ptr))
            (alloc-code [length]
              (.malloc exports (* 4 length)))
            (free [ptr]
              (.free exports ptr))
            (code->wasm-code [code]
              (let [ptr (alloc-code (count code))
                    arr (js/Int32Array. buffer ptr (count code))]
                (.set arr (clj->js (encode-code-fn code)))
                ptr))]

      (let [solver-ptr (solver-ctor (count valid-symbols) code-length)
            secret-ptr (alloc-code code-length)]
        (doseq [{:keys [guess feedback]} history]
          (let [code-ptr (code->wasm-code guess)]
            (add-interaction solver-ptr code-ptr (:fm feedback) (:pm feedback))
            (free code-ptr)))
        (let [solution-exists (= (solve solver-ptr secret-ptr) 1)]
          (solver-dtor solver-ptr)
          (when solution-exists
            (let [secret (-> (js/Int32Array. buffer secret-ptr code-length)
                             decode-code-fn)]
              (free secret-ptr)
              secret)))))))

(defrecord SatBasedSolver []
  Solver
  (load [_] (load-wasm))
  (loaded? [_] @wasm-atom)
  (solve [_ config history] (solve config history)))
