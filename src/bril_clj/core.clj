(ns bril-clj.core
  (:require [clojure.data.json :as json]
            [bril-clj.graphviz :as graphviz]
            [bril-clj.optimizationis.dead-code :as opt-dc]
            [bril-clj.bril :as bril]
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; tmp
(def bril-json (:functions (bril/bril->json "../bril/test/interp/core/add-overflow.bril")))
(def body   (:instrs (bril-json 0)))


(def cfg (bril/bril-json->cfg bril-json))
