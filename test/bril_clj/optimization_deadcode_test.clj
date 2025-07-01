(ns bril-clj.optimization-deadcode-test
  (:require [clojure.test :refer :all]
            [bril-clj.consts :as consts]
            [bril-clj.utils :as t]
            [bril-clj.optimizations.dead-code :as DCE]
            [bril-clj.optimizations.core :as opt]
            [bril-clj.bril :as bril]))

(deftest DCE-unused-variable-declarations
  (testing "DCE-unused-variable-declarations till convergence should exhibit similar behavior before convergence"
    (doall (t/compare-optimization-to-unoptimized
            consts/test-bril-files
            (partial
             opt/apply-function-optimization-until-convergence
             DCE/function|DCE-unused-variable-declarations)))))

(deftest DCE-double-assignment
  (testing "DCE-double-assignment till convergence should exhibit similar behavior before convergence"
    (doall (t/compare-optimization-to-unoptimized
            consts/test-bril-files
            (partial
             opt/apply-block-optimization-until-convergence
             DCE/block|DCE-double-assignment)))))
