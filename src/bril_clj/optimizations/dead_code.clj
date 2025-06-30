(ns bril-clj.optimizations.dead-code)

(defn DCE-double-assignment
  "Dead Code Elemenation: unused variable declarations
  Block level optimization.
  It removes unused variable declarations.
  ```
  main {
    a: int = const 4;
    a: int = const 4;
    print a;
  }
  ```
  first `a` should be removed.
  "
  [block])

(defn DCE-unused-variable-declarations
  "Dead Code Elemenation: unused variable declarations
  Block level optimization.
  It removes unused variable declarations.
  ```
  main {
    a: int = const 4;
    b: int = const 4;
    print b;
  }

  ```
  `a` should be removed.

  Example Input:
  ```
  ({:dest \"v1\", :op \"const\", :type \"int\", :value 2}
   {:dest \"v3\", :op \"const\", :type \"int\", :value 3}
   {:args [\"v0\" \"v1\"], :dest \"v2\", :op \"add\", :type \"int\"}
   {:args [\"v2\"], :op \"print\"}])
  ```
  Example Output:
  ```
  ({:dest \"v0\", :op \"const\", :type \"int\", :value 1}
   {:dest \"v1\", :op \"const\", :type \"int\", :value 2}
   {:args [\"v0\" \"v1\"], :dest \"v2\", :op \"add\", :type \"int\"})
  ```

  "
  [block]
  (->> block
       (map :args)
       (remove nil?)
       (flatten)
       (into #{})
       ;; Above we end up with all used arg set!
       ;; Now we need to remove all `:dest` that isn't in `block`
       ((fn [used]
          (remove #(and (not (used (:dest %)))
                       (not (nil? (:dest %)))) block)))))
