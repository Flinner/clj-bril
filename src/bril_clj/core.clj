(ns bril-clj.core
  (:require [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn bril2json
  "shell equivelent of bril2json < $filename"
  [filename]
  (json/read-str
   (:out (sh "/home/lambda/.local/bin/bril2json" :in (slurp filename)))
   :key-fn keyword))

; tmp
(def bril-json (:functions (bril2json "../bril/test/interp/core/jmp.bril")))
(def body (:instrs (bril-json 0)))

(defn mycfg
  [bril-json]
  (for [func bril-json :let [instrs (:instrs func)]] (form-blocks instrs)))

; Helper functions
(defn control-instr?
  [instr]
  (some #(= (:op instr) %) '("jmp" "br" "call" "ret")))

; These Terminate Blocks
(defn terminator-instr?
  [instr]
  (some #(= (:op instr) %) '("jmp" "br" "call" "ret")))
(defn label-instr? [instr] ((complement nil?) (:label instr)))

;; Move to Testing??
(control-instr? {:dest "v", :op "const", :type "int", :value 4})
(control-instr? {:labels ["somewhere"], :op "jmp"})
(label-instr? {:label "somewhere"})
(label-instr? {:labels ["somewhere"], :op "jmp"})

(defn form-blocks
  [body]
  (loop [cur-block []
         blocks []
         [instr & remaining-body] body]
    (if-not instr
      (conj blocks cur-block) ; in last iteration, merge last block with rest of blocks
      (let [cur-block+cur-instr  (conj cur-block instr)
            blocks+cur-block     (conj blocks cur-block)
            blocks+cur-block+cur-instr (conj blocks cur-block+cur-instr)]
        (cond (terminator-instr? instr) (recur [] blocks+cur-block+cur-instr remaining-body)
              (label-instr? instr) (recur [instr] blocks+cur-block remaining-body)
              :else (recur cur-block+cur-instr blocks remaining-body))))))

(defn form-cfg-by-index
  "Takes a Vector of blocks, see [[form-blocks]], and returns a cfg-by-index.
  Later, you can use [[from-cfg-map]] to convert it to hash map shape.

  Example Input:
  ```clojure
  [
    [{:dest \"v\", :op \"const\", :type \"int\", :value 4} {:labels [\"somewhere\"], :op \"jmp\"}]
    [{:dest \"v\", :op \"const\", :type \"int\", :value 2}]
    [{:label \"somewhere\"} {:args [\"v\"], :op \"print\"}]
  ]
  ```
  Output:
  ```clojure
  [
   {:label b0, :succ [2]          :block [...]}          ; This is it `jmp`s to \"somewhere\"
   {:label b1, :succ [2]          :block [...]}          ; This is because it doesn't jmp anywhere
   {:label \"somewhere\", :succ [] :block [...]}          ; Exit!
  ]
  ```
  "
  [blocks]
  (def labels-to-idx (into {} (keep-indexed #(when (:label (first %2)) {(:label (first %2)) %1}) blocks)))

  ; Here is how the data in `cfg-indexed` looks now:
  ; [
  ;   {:label b0, :succ [2]          :block [...]}))          ; This is it `jmp`s to \"somewhere\"
  ;   {:label b1, :succ [2]          :block [...]}))          ; This is because it doesn't jmp anywhere
  ;   {:label \"somewhere\", :succ [] :block [...]}))          ; Exit!
  ; ]
  ; we need to convert it into a `hash-map` and resolve the `:succ`
  (def cfg-indexed
    (vec (map-indexed (fn [idx, block]
                        {:label (keyword (if-let [predefined-label (:label (first block))]
                                           predefined-label
                                           (str "b" idx))) ; Label is of the first block, if any!
                         :succ  (if-let [labels (:labels (last block))]
                                  (mapv labels-to-idx labels) ; convert to idx if they exist, (it SHOULD exist).
                                  [(if-not (= (inc idx) (count blocks)) (+ idx 1))]) ; it is just idx+1; unless for last element is nil.
                         :block block}) blocks)))
  (->> cfg-indexed
       (mapv (fn [block]
               (update block :succ (partial mapv #(get-in cfg-indexed [% :label])))))
    ;; ({:label :b0,
    ;;   :succ [:somewhere],
    ;;   :block [...]}
    ;;  {:label :b1,
    ;;   :succ [:somewhere],
    ;;   :block [...]}
    ;;  {:label :somewhere,
    ;;   :succ [nil],
    ;;   :block [...]})
       (mapv (fn [{:keys [label succ block]}]
               [label {:block block :succ succ}]))
    ;; [[:b0
    ;;   {:block [...],
    ;;    :succ [:somewhere]}]
    ;;  [:b1
    ;;   {:block [...],
    ;;    :succ [:somewhere]}]
    ;;  [:somewhere
    ;;   {:block [...],
    ;;    :succ [nil]}]]
       flatten
       ((partial apply hash-map))))

(form-cfg-by-index (form-blocks body))
