(ns bril-clj.optimizations.dead-code
  (:require [bril-clj.optimizations.core :as opt]))

(defn block|DCE-double-assignment
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

(defn function|DCE-unused-variable-declarations
  "Dead Code Elemenation: unused variable declarations
  Function level optimization.
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
  ; NOTE: IT TAKES A SINGLE FUNCTION
  ({:name \"main\",
    :args nil,
    :type nil,
    :instrs
    [{:label :b0,
      :succ [:label],
      :block
      [{:dest \"v0\", :op \"const\", :type \"int\", :value 1}
       {:dest \"UNUSED\", :op \"const\", :type \"int\", :value 2}
       {:dest \"v1\", :op \"const\", :type \"int\", :value 2}
       {:labels [\"label\"], :op \"jmp\"}]}
     {:label :b1, :succ [:label], :block []}
     {:label :label,
      :succ [nil],
      :block
      [{:label \"label\"}
       {:args [\"v0\" \"v1\"], :dest \"v2\", :op \"add\", :type \"int\"}
       {:args [\"v2\"], :op \"print\"}]}]})
  ```
  Example Output:
  ```
  ;; Same as input, but with \"UNUSUED\" removed

  ```

  "
  [function]
  (let [used (->> :instrs
                  function
                  (map :block)
                  (flatten)
                  (map :args)
                  (remove nil?)
                  (flatten)
                  (into #{}))]
       ;; Above we end up with all used arg set!
       ;; Now we need to remove all `:dest` that isn't in `block`
       (opt/apply-block-optimization-to-function-once
        (fn [block]
         (remove #(and (not (used (:dest %)))
                       (= "const" (:op %)))
                 block))
        function)))
