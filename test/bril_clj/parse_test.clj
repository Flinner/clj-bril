(ns bril-clj.parse-test
  (:require [clojure.test :refer :all]
            [bril-clj.consts :as consts]
            [bril-clj.utils :as t]
            [bril-clj.bril :as bril]
            [bril-clj.core :refer :all]))


(deftest txt->json=txt->json->cfg->json
  (testing "Original JSON and JSON->CFG->JSON should execute similiarly"
    (doall (t/compare-fns consts/test-bril-files
                        (comp bril/brili<-json bril/txt->json)
                        (comp bril/brili<-json bril/cfg->json bril/json->cfg bril/txt->json)))))
