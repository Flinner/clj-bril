(ns bril-clj.optimizations.core)

(defn apply-block-optimization-once
  [f cfg]
  (map (fn [func]
         (update func
                 :instrs
                 (fn [instrs]
                   (map #(update % :block f) instrs))))
       cfg))

(defn apply-block-optimization-until-convergence
  [f cfg]
  (loop [past (apply-block-optimization-once f cfg)]
    (let [now (apply-block-optimization-once f past)]
      (if (= now past)
        now
        (recur now)))))
  
