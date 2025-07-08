(ns bril-clj.graphviz
  (:require [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn cfg->graphviz
  "Convert a CFG to graphviz string"
  [cfg]
  (->> cfg
       (mapcat :instrs)
      ;; [[:b0
      ;;   {:block [...],
      ;;    :succ [:somewhere]}]
      ;;  [:b1
      ;;   {:block [...],
      ;;    :succ [:somewhere]}]
      ;;  [:somewhere
      ;;   {:block [...],
      ;;    :succ [nil]}]]

       (map (fn [block]
              {:label (:label block), :succ (:succ block)}))
      ;; ({:label :b1, :succ [:somewhere]}
      ;;  {:label :b0, :succ [:somewhere]}
      ;;  {:label :somewhere, :succ [nil]})
       (map (fn [{:keys [label succ]}]
              (map #(if % (str (name label) " -> " (name %) ";\n")) succ)))
       flatten
      ;; ":b1 -> :somewhere;\n:b0 -> :somewhere;\n"
       (apply str)
       (#(str "digraph" " " "EXAMPLE_TODO" " " "{\n" % "}"))))

(defn graphviz->feh [graphviz]
  (future
    (sh "/bin/bash" "-c" (str "echo \"" graphviz "\" | dot -Tpng | feh -"))))

;; (graphviz->feh (cfg->graphviz cfg))
