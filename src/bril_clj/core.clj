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
    (:out (sh "/home/lambda/.local/bin/bril2json" :in (slurp filename)))))


; tmp
(def bril-json ((bril2json "../bril/test/interp/core/jmp.bril") "functions"))
(def body ((bril-json 0) "instrs"))

(defn mycfg
  [bril-json]
  (for [func bril-json :let [instrs (func "instrs")]] (form-blocks instrs)))

; Helper functions
(defn control-instr?
  [instr]
  (some #(= (instr "op") %) '("jmp" "br" "call" "ret")))
(defn label-instr? [instr] ((complement nil?) (instr "label")))


(control-instr? {"dest" "v", "op" "const", "type" "int", "value" 2})
(control-instr? {"labels" ["somewhere"], "op" "jmp"})
(label-instr? {"label" "somewhere"})
(label-instr? {"labels" ["somewhere"], "op" "jmp"})

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
        (cond (control-instr? instr)
              (recur [] blocks+cur-block+cur-instr remaining-body)
              (label-instr? instr)
              (recur [instr] blocks+cur-block remaining-body)
              :else (recur cur-block+cur-instr blocks remaining-body))))))

(form-blocks body)




(for [func bril-json :let [instrs (get func "instrs")]] (form-blocks instrs))
