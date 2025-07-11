(ns bril-clj.optimizations.lvn
  (:require [bril-clj.bril :as bril]))

(declare
 value-exists
 var-will-be-overwritten
 args-from-table
 subsitute-lvn-dest
 subsitute-lvn-op
 subsitute-lvn-args
 block->lvn-table
 optimize-lvn-table
 resolve-arg
 resolve-arg-recursively
 optimize:copy-propagation-lvn-args
 optimize:constant-propagation-lvn-args
 optimize:constant-folding
 lvn-table->instrs)

(defn lvn
  [block]
  (->> block
       block->lvn-table
       optimize-lvn-table
       lvn-table->instrs))

(defn- optimize-lvn-table
  "Restore the LVN table back to instrs"
  ;; Example Data Input
  ;; [{ :idx 0
  ;;    :variable "a-0",
  ;;    :value {:op "const", :args (), :value 4},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 4}},
  ;;   {:variable "sum1-1",
  ;;    :idx 1
  ;;    :value {:op "add", :args (0 0), :value nil},
  ;;    :original-instr {:args ["a" "b"], :dest "sum1", :op "add", :type "int"}},
  ;;   {:variable "a-2",
  ;;    :idx 2
  ;;    :value {:op "const", :args (), :value 8},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 8}}] 
  [lvn-table]
  (->> lvn-table
       (map (partial optimize:copy-propagation-lvn-args lvn-table))
       (map (partial optimize:constant-propagation-lvn-args lvn-table))
       (map (partial optimize:constant-folding lvn-table))))

(defn- lvn-table->instrs
  "Restore the LVN table back to instrs"
  ;; Example Data Input
  ;; [{ :idx 0
  ;;    :variable "a-0",
  ;;    :value {:op "const", :args (), :value 4},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 4}},
  ;;   {:variable "sum1-1",
  ;;    :idx 1
  ;;    :value {:op "add", :args (0 0), :value nil},
  ;;    :original-instr {:args ["a" "b"], :dest "sum1", :op "add", :type "int"}},
  ;;   {:variable "a-2",
  ;;    :idx 2
  ;;    :value {:op "const", :args (), :value 8},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 8}}] 
  [lvn-table]
  (->> lvn-table
       (map (partial subsitute-lvn-dest lvn-table))
       (map (partial subsitute-lvn-args lvn-table))
       (map (partial subsitute-lvn-op   lvn-table))
       (map :original-instr)))

(defn- block->lvn-table
  "This returns the lvn's `table`
  This function takes a `:block`.
  But DO NOT  use [[apply-function-optimization-once]], just pass a `:block`
  of a `cfg` on it.

  table = mapping from value tuples to canonical variables,
  with each row numbered
  var2num = mapping from variable names to their current
  value numbers (i.e., rows in table)

  Algorithm:
  for instr in block:
      value = (instr.op, var2num[instr.args[0]], ...)

      if value in table:
          # The value has been computed before; reuse it.
          num, var = table[value]
          replace instr with copy of var

      else:
          # A newly computed value.
          num = fresh value number

          dest = instr.dest
          if instr will be overwritten later:
               dest = fresh variable name
               instr.dest = dest
          else:
               dest = instr.dest

          table[value] = num, dest

          for a in instr.args:
              replace a with table[var2num[a]].var

      var2num[instr.dest] = num"
  [block]
  (loop [table []
         var2num {}
         [instr & instrs] block]
         ;; Rename vars to avoid overwrites
    (let [new-idx (count table)
          ;; new name only if will be overwritten
          ;; this caused much pain later, so I will explain
          ;; if you rename everything, the next block will be screwed
          ;; Example
          ;; @main {
          ;;   a: int = const 1;
          ;; jmp .label;
          ;; .label:
          ;;   print a;
          ;; }
          ;;
          ;; It will get split to two blocks. a will be renamed,
          ;; and print won't be aware!
          var-name (if (var-will-be-overwritten instr instrs)
                     (str (:dest instr) "-" new-idx)
                     (:dest instr))
          ;; new var2name
          var2num' (assoc var2num (:dest instr) new-idx)
          ;; *might* not be added
          new-table-row {:idx new-idx
                         :variable  var-name
                         :value
                         {:op   (:op instr)
                          :args (map var2num
                                     ;; commutative get their arguments sorted
                                     ;; enhancment could be made to change a < b to b > a
                                     ;; I aint doing these "enhancments"! I am in a hurry :(
                                     ((if (bril/commutative-instr? instr) sort identity)
                                      (:args instr)))
                          :value  (:value instr)}
                         :original-instr instr}
          ;; if the variable exists, get its index
          var2num-idx (value-exists new-table-row table)
          table' (conj table
                        ;new-idx
                       new-table-row)]
      (cond
       ;; end `recur`
        (nil? instr)    table

        ;; terminators/labels get added to the table.
        (bril/terminator-instr? instr) (recur table' var2num instrs)
        (bril/label-instr?      instr) (recur table' var2num instrs)

        ;; The value has been computed before, reuse it.
        var2num-idx (recur table'
                           (assoc var2num (:dest instr) var2num-idx)
                           instrs)
        :else ;; Newly Computed Value
        (recur table'
               ;; non vars (labels/control instructions)
               ;; don't update the var2num
               (if var-name var2num' var2num)
               instrs)))))

(defn- value-exists
  "If the value exists return its index, otherwise `nil`
  Example Input:
  ```
   [{:idx 0 :variable \"v0-0\", :value {:op \"const\", :args (), :value 1}},
    {:idx 1 :variable \"v1-1\", :value {:op \"const\", :args (), :value 2}},
    {:idx 2 :variable \"v1-2\", :value {:op \"const\", :args (), :value 3}},
    {:idx 3 :variable \"v1-3\", :value {:op \"const\", :args (), :value 4}},
    {:idx 4 :variable \"v2-4\", :value {:op \"add\", :args (0 3), :value nil}},
    {:idx 5 :variable \"v3-5\", :value {:op \"add\", :args (0 3), :value nil}}]
  ```
  "
  [new-table-row table]
  (some (fn [row] (if (= (:value new-table-row)
                         (:value row))
                    (:idx row)))
        table))

(defn var-will-be-overwritten
  [instr instrs]
  (if-let [var (:dest instr)]
    (some #(= (:dest %) var) instrs)))

(defn- optimize:copy-propagation-lvn-args
  "Chains of assignment can directly refer to the original variable.
  
  ```python
  @main {
      x: int = const 4;
      copy1: int = id x;
      copy2: int = id copy1;
      copy3: int = id copy2;
      print copy3;
  }
  ```

  Gets optimized with copy propagation to:
  ```python
  @main {
      x: int = const 4;
      copy1: int = id x;
      copy2: int = id x;
      copy3: int = id x;
      print copy3;
  }
  ```"
  [lvn-table lvn-row]
  (let [original-instr (:original-instr lvn-row)
        original-args  (:args original-instr)
        args-idxs      (:args (:value lvn-row))]
    (if-not original-args
     ;; doesn't have args, do nothing.
      lvn-row
     ;; else
      (update-in lvn-row
                 [:value :args]
                 (fn [old-args]
                   (map (partial resolve-arg-recursively lvn-table) old-args))))))

(defn- optimize:constant-propagation-lvn-args
  "Similar to constant-propagation, but the constant itself propagates.
  
  ```python
  @main {
      x: int = const 4;
      copy1: int = id x;
      copy2: int = id copy1;
      copy3: int = id copy2;
      print copy3;
  }
  ```
  Becomes
  ```python
  @main {
      x:     int = const 4;
      copy1: int = const 4;
      copy2: int = const 4;
      copy3: int = const 4;
      print copy3;
  }
  ```"
  [lvn-table lvn-row]
  (let [original-instr   (:original-instr lvn-row)
        original-args    (:args original-instr)
        args-idx         (first (:args   (:value lvn-row)))
        new-row          (resolve-arg lvn-table args-idx)
        new-is-const     (= "const" (:op (:value new-row)))
        old-is-id        (= "id"    (:op (:value lvn-row)))]
    (if-not (and new-is-const old-is-id)
     ;; isn't a constant propagation.
      lvn-row
     ;; else
      (assoc lvn-row
             :value (:value new-row)))))

(defn- optimize:constant-folding
  "Fold constants! Compile time operations!
  
  ```python
  @main {
      a: int   = const 4;
      b: int   = const 1;

      sum: int = add a b;
      print sum;
  }
  ```

  Gets optimized with constant folding
  ```python
  @main {
     sum: int = const 5;
     print sum;
  }
  ```"
  [lvn-table lvn-row]
  (let [op-fn ({"add" +, "mul" *, "sub" -, "div" /} (:op (:value lvn-row)))
        args (->> (-> lvn-row :value :args)                 ;; this resolves to arg indices (0 1)
                  (map     (partial resolve-arg lvn-table)) ;; resolves to instructions
                  (map    #(get-in % [:value :value])))]    ;; gets the values...
    (if (and op-fn (not-any? nil? args))
      ;; if is a math op, perform math operation
      (->> (apply op-fn args)
          ;; construct the new instruction
           (hash-map :op "const" :value)
          ;; then replace the instruction
           (assoc lvn-row :value))
      ;; else... not an arthimetic operation
      lvn-row)))

(defn- resolve-arg-to-variable
  [lvn-table arg-idx]
  (get (first (filter #(= arg-idx (:idx %)) lvn-table)) :variable))

(defn- resolve-arg
  [lvn-table arg-idx]
  (first (filter #(= arg-idx (:idx %)) lvn-table)))

(defn- resolve-arg-recursively
  [lvn-table old-arg-idx]
  (let [lvn-row (first (filter #(= old-arg-idx (:idx %)) lvn-table))
        new-idx (first (:args (:value lvn-row)))
        is-id   (= "id" (:op (:value lvn-row)))]
    (if (and is-id new-idx)
      (resolve-arg-recursively lvn-table new-idx)
      old-arg-idx)))

(defn- subsitute-lvn-args
  "Change original args to the args from table"
  [lvn-table lvn-row]
  (if-not (:args (:original-instr lvn-row))
    ;; doesn't have args, do nothing.
    lvn-row
    ;; else
    (assoc-in lvn-row
              [:original-instr :args]
              ;; this is madness i am losing my mind!!!
              (let [original-args (get-in lvn-row [:original-instr :args])
                    modified-args (map (partial resolve-arg-to-variable lvn-table)
                                       (get-in lvn-row [:value :args]))]
                ;; if some of the modified args are nil, it means I should use the original args
                (cond (some nil? modified-args) original-args
                      (empty? modified-args) nil
                      :else modified-args)))))

(defn- subsitute-lvn-dest
  "Change original dest to the dest from table"
  [lvn-table lvn-row]
  (if-not (:dest (:original-instr lvn-row))
    ;; doesn't have dest, do nothing.
    lvn-row
    ;; else
    (assoc-in lvn-row
              [:original-instr :dest]
              (:variable lvn-row))))

(defn- subsitute-lvn-op
  "Change original op to the op from table"
  [lvn-table lvn-row]
  (if (= (:op (:value          lvn-row))
         (:op (:original-instr lvn-row)))
    ;; operation didn't change, do anything
    lvn-row
    ;; else
    (assoc lvn-row
           :original-instr
           (->> (merge
                 (:original-instr lvn-row)
                 (:value lvn-row))))))
                 ;; sometimes we end up :args (), empty, I filter it


