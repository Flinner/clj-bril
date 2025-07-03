(ns bril-clj.optimizations.core)

(defn apply-block-optimization-to-cfg-once
  [f cfg]
  (map (fn [func]
         (update func
                 :instrs
                 (fn [instrs]
                   (map #(update % :block f) instrs))))
       cfg))

(defn apply-block-optimization-to-function-once
  [f func]
  (update func
          :instrs
          (fn [instrs]
            (map #(update % :block f) instrs))))

(defn apply-function-optimization-once
  [f cfg]
  (map f cfg))

(defn apply-until-convergence
  [f cfg]
  (loop [past (f cfg)]
    (let [now (f past)]
      (if (= now past)
        now
        (recur now)))))

(defn apply-function-optimization-until-convergence
  [f cfg]
  (apply-until-convergence
   (partial apply-function-optimization-once f)
   cfg))

(defn apply-block-optimization-until-convergence
  [f cfg]
  (apply-until-convergence
   (partial apply-block-optimization-to-cfg-once f)
   cfg))

