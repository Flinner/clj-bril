(ns bril-clj.utils
  (:require [clojure.test :refer :all]
            [bril-clj.bril :as bril]))

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

(defn compare-optimization-to-unoptimized
  [data f]
  (compare-fns data
               (comp bril/brili<-json bril/txt->json)
               (comp bril/brili<-cfg
                     f
                     bril/json->cfg
                     bril/txt->json)))
