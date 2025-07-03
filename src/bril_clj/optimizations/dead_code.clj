(ns bril-clj.optimizations.dead-code
  (:require [bril-clj.optimizations.core :as opt]))

(defn block|DCE-double-assignment
  "dead code elemenation: unused variable declarations
  block level optimization.
  it removes unused variable declarations.
  ```
  main {
    a: int = const 4;
    a: int = const 4;
    print a;
  }
  ```
  first `a` should be removed.
  "
  ;;; logic:
  ;; def_not_used = {} # definitions not used
  ;; for instr in instrs
  ;; check_use
  ;;   def_not_used -= instr.args
  ;;
  ;;  check def
  ;;   if instr.def in def_not_used
  ;;      delete def_not_used[instr.def]
  ;;   def_not_used[instr.def] = instr
  [block]
  (loop [[instr & instrs :as block-indexed] (map-indexed #(assoc %2 :idx %1) block)
         to-keep-idxs #{}
         def-not-used {}]
    (let [def-not-used-now (apply dissoc def-not-used (:args instr))
          def-not-used-future (assoc def-not-used-now (:dest instr) (:idx instr))]
      (if-not instr
        ;; use `t
        (keep-indexed #(if (to-keep-idxs %1) %2) block)
        ;; to-keep-idxs
        ;; 
        (if-not (= "const" (:op instr))
          ;; Keep all non defs, they are non of this optimization's business
          (recur instrs (conj to-keep-idxs (:idx instr)) def-not-used-future)
          ;; defs that are
          (recur instrs
                 (if (and (def-not-used-now (:dest instr)))
                   ;; discard last instruction and keep this
                   (conj (disj to-keep-idxs (def-not-used-now (:dest instr)))
                         (:idx instr))
                   (conj to-keep-idxs (:idx instr)))
                 def-not-used-future))))))

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
  (let [used (->> function
                  :instrs
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
