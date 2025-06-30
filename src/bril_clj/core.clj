(ns bril-clj.core
  (:require [clojure.data.json :as json]
            [bril-clj.graphviz :as graphviz]
            [bril-clj.optimizations.dead-code :as opt-dc]
            [bril-clj.bril :as bril]
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; tmp
(def bril-json (bril/txt->json "./test/bril_files/parse/add.bril"))
(def body   (:instrs (bril-json 0)))


(def cfg (bril/json->cfg bril-json))
(bril/brili<-cfg cfg)

(bril/brili<-json (bril/txt->json "../bril/test/interp/core/add-overflow.bril"))
(bril/json->txt (bril/txt->json "../bril/test/interp/core/add-overflow.bril"))
