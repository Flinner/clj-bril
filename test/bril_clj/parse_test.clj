(ns bril-clj.parse-test
  (:require [clojure.test :refer :all]
            [bril-clj.consts :as consts]
            [bril-clj.bril :as bril]
            [bril-clj.core :refer :all]))

(defn compare-fns
  [data f g]
  (->> data
       ;; repeat 3 times.
       ;; (filename, original json, after modification)
       (map #(list %
                   (f %)
                   (g %)))
       (map #(is (= (second %) (nth % 2)) 
                 (str "Error caused by: " (first %))))))


(deftest txt->json=txt->json->cfg->json
  (testing "Original JSON and JSON->CFG->JSON should execute similiarly"
    (doall (compare-fns consts/test-bril-files
                        (comp bril/brili<-json bril/txt->json)
                        (comp bril/brili<-json bril/cfg->json bril/json->cfg bril/txt->json)))))
