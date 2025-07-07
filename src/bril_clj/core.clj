(ns bril-clj.core
  (:require [clojure.data.json :as json]
            [bril-clj.graphviz :as graphviz]
            [bril-clj.utils :as utils]
            [bril-clj.optimizations.lvn :as optlvn]
            [bril-clj.optimizations.dead-code :as optdc]
            [bril-clj.optimizations.core :as opt]
            [bril-clj.bril :as bril]
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;; (def bril-json (bril/txt->json "./test/bril_files/parse/positions.bril"))
;; (def cfg (bril/json->cfg bril-json))

;; (def body
;;   [{:label :b0,
;;     :succ [:label],
;;     :block
;;     [{:dest "a", :op "const", :type "int", :value 4}
;;      {:dest "b", :op "const", :type "int", :value 4}
;;      {:args ["a" "b"], :dest "sum1", :op "add", :type "int"}
;;      {:dest "a", :op "const", :type "int", :value 8}
;;      {:args ["a" "b"], :dest "sum2", :op "add", :type "int"}
;;      {:args ["sum1" "sum2"], :dest "prod", :op "mul", :type "int"}
;;      {:args ["prod"], :op "print"}
;;      {:label :b1, :succ [:label], :block []}]}
;;    {:label :label,
;;     :succ [nil],
;;     :block
;;     [{:label "label"}
;;      {:args ["v0" "v1"], :dest "v2", :op "add", :type "int"}
;;      {:args ["v2"], :op "print"}]}])

(->> "./test/bril_files/parse/float.bril"
     bril/txt->json
     bril/json->cfg
     ((partial opt/apply-block-optimization-to-cfg-once          optlvn/lvn))
     ;; ((partial opt/apply-block-optimization-until-convergence    optdc/block|DCE-double-assignment))
     ;; ((partial opt/apply-function-optimization-until-convergence optdc/function|DCE-unused-variable-declarations))
     bril/cfg->json
     bril/json->txt
     println)
