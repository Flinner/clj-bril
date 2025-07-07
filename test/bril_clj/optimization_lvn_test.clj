(ns bril-clj.optimization-lvn-test
  (:require [clojure.test :refer :all]
            [bril-clj.consts :as consts]
            [bril-clj.utils :as t]
            [bril-clj.optimizations.lvn :as optlvn]
            [bril-clj.optimizations.dead-code :as DCE]
            [bril-clj.optimizations.core :as opt]
            [bril-clj.bril :as bril]))

(deftest lvn+DCE-unused-variable-declarations
  (testing "LVN+DCE-unused-variable-declarations till convergence should exhibit similar behavior to unoptimized"
    (doall (t/compare-optimization-to-unoptimized
            consts/test-bril-files
            (comp
             (partial
              opt/apply-function-optimization-until-convergence
              DCE/function|DCE-unused-variable-declarations)
             (partial opt/apply-block-optimization-to-cfg-once optlvn/lvn))))))

(deftest lvn+DCE-double-assignment
  (testing "LVN+DCE-double-assignment till convergence should exhibit similar behavior to unoptimized"
    (doall (t/compare-optimization-to-unoptimized
            consts/test-bril-files
            (comp
             (partial
              opt/apply-block-optimization-until-convergence
              DCE/block|DCE-double-assignment)
             (partial opt/apply-block-optimization-to-cfg-once optlvn/lvn))))))
