(ns bril-clj.optimizations.lvn
  (:require [bril-clj.optimizations.core :as opt]
            [bril-clj.bril :as bril]))

(declare
 value-exists
 var-will-be-overwritten
 subsitute-lvn-args
 args-from-table
 subsitute-lvn-dest
 block->lvn-table
 lvn-table->instrs)

(defn lvn
  [block]
  (->> block
       block->lvn-table
       lvn-table->instrs))

(defn- lvn-table->instrs
  "Restore the LVN table back to instrs"
  ;; Example Data Input
  ;; ({0
  ;;   {:variable "a-0",
  ;;    :value {:op "const", :args (), :val 4},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 4}},
  ;;   1
  ;;   {:variable "sum1-1",
  ;;    :value {:op "add", :args (0 0), :val nil},
  ;;    :original-instr {:args ["a" "b"], :dest "sum1", :op "add", :type "int"}},
  ;;   2
  ;;   {:variable "a-2",
  ;;    :value {:op "const", :args (), :val 8},
  ;;    :original-instr {:dest "a", :op "const", :type "int", :value 8}},
  ;;   3
  ;;   {:variable "sum2-3",
  ;;    :value {:op "add", :args (2 0), :val nil},
  ;;    :original-instr {:args ["a" "b"], :dest "sum2", :op "add", :type "int"}},
  ;;   4
  ;;   {:variable "prod-4",
  ;;    :value {:op "mul", :args (1 3), :val nil},
  ;;    :original-instr
  ;;    {:args ["sum1" "sum2"], :dest "prod", :op "mul", :type "int"}}})
  [lvn-table]
  (->> lvn-table
       (map (partial subsitute-lvn-args lvn-table))
       (map (partial subsitute-lvn-dest lvn-table))
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
                         :value    {:op   (:op                      instr)
                                    :args (map var2num
                                               ;; commutative get their arguments sorted
                                               ;; enhancment could be made to change a < b to b > a
                                               ;; I aint doing these "enhancments"! I am in a hurry :(
                                               ((if (bril/commutative-instr? instr) sort identity)
                                                (:args instr)))
                                    :val  (:value                   instr)}
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
   {0 {:variable \"v0-0\", :value {:op \"const\", :args (), :val 1}},
    1 {:variable \"v1-1\", :value {:op \"const\", :args (), :val 2}},
    2 {:variable \"v1-2\", :value {:op \"const\", :args (), :val 3}},
    3 {:variable \"v1-3\", :value {:op \"const\", :args (), :val 4}},
    4 {:variable \"v2-4\", :value {:op \"add\", :args (0 3), :val nil}},
    5 {:variable \"v3-5\", :value {:op \"add\", :args (0 3), :val nil}}}
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
                    modified-args (map (fn [arg]
                                         (get (lvn-table arg) :variable arg))
                                       (get-in lvn-row [:value :args]))]
                ;; if some of the modified args are nil, it means lol
                (if (some nil? modified-args)
                  original-args
                  modified-args)))))

(defn- subsitute-lvn-dest
  "Change original dest to the dest from table"
  [lvn-table lvn-row]
  (if-not (:dest (:original-instr lvn-row))
    ;; doesn't have args, do nothing.
    lvn-row
    ;; else
    (assoc-in lvn-row
              [:original-instr :dest]
              (:variable lvn-row))))
