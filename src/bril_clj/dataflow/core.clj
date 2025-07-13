(ns bril-clj.dataflow.core)

;;; References:
;; See: https://www.cs.cornell.edu/courses/cs6120/2025sp/lesson/4/

(declare kills-set defs-instrs)

(defn worklist
  "Takes any arbitrary FIXME

  ```
  in[entry] = init
  out[*] = init
  
  worklist = all blocks
  while worklist is not empty:
      b = pick any block from worklist
      in[b] = merge(out[p] for every predecessor p of b)
      out[b] = transfer(b, in[b])
      if out[b] changed:
          worklist += successors of b
  ```

  Example Input: (a CFG)
  Example Output (a DFG):
  ```clojure
  {:b0
   {:in  [{:dest \"a\", :op \"const\", :type \"int\", :value 1}, ...]
    :out [...]}}
  ```
  
  "
  [CFG]
  (loop [[b & rest] (mapcat :instrs CFG)
         DFG {}]
    (if-not b
      DFG
      (let [label (:label b)
            in (in-f CFG DFG label)
            out (out-f b in)
            DFG' (assoc DFG label {:in in, :out out})]
        (if (= (:out (get DFG label))
               (:out (get DFG' label)))
          (recur rest DFG')
          (recur (into (get-succ-blocks CFG label) rest) DFG'))))))

(defn worklist->dests [worklist]
  (->> worklist
       (map (fn [[label {:keys [in out]}]]
              [label {:in (map :dest in)
                      :out (map :dest out)}]))))

;; FIXME: remove. Unique Union
;; (def merge-blocks (comp distinct concat))

(defn in-f [CFG DFG label]
  (->>
   ;; start with pred list, say [:b0 :b15]
   (get-preds CFG label)
   (mapcat #(:out (% DFG)))
   distinct))

(defn get-block
  "Returns Block as a whole.
  Example: 
  ```
  (get-block cfg :b0)
  ```
  Output:
  ```
  {:label :b0,
  :succ [:start],
  :block [{:labels [\"start\"], :op \"jmp\"}],
  :pred []}
  ```
"
  [CFG label]
  (->> CFG
       (mapcat :instrs)
       (filter #(= label (:label %)))
       first))

(defn get-preds [CFG label]
  (:pred (get-block CFG label)))

(defn get-succs [CFG label]
  (:succ (get-block CFG label)))

(defn get-succ-blocks [CFG label]
  (map (partial get-block CFG) (get-succs CFG label)))

(defn out-f [block in]
  (let [defs (defs-instrs (:block block))
        kills (kills-set (:block block))]
    (->> in
         (remove (comp kills :dest))
         (concat defs))))

(defn defs-instrs [block]
  (->> block
       (filter :dest)))

(defn kills-set
  "Given a block, it returns the set of killed defs.
  Example Input:
  ```clj
  (def block [{:label \"l_1_2\"}
              {:dest \"x_1_2\", :op \"const\", :type \"int\", :value 0}
              {:labels [\"l_1_3\"], :op \"jmp\"}])
  ```
  Example output:
  ```clj
  #{\"x_1_2\"})
  ```
  "
  [block]
  (->> block
       (defs-instrs)
       (map :dest)
       set))

;; FIXME: Remove ALL under this...

(def in    [{:dest "a", :op "const", :type "int", :value 1}
            {:dest "b", :op "const", :type "int", :value 2}
            {:dest "c", :op "const", :type "int", :value 3}])

(def block [{:label "l_1_2"}
            {:dest "a", :op "const", :type "int", :value 9}
            {:dest "x_1_3", :op "const", :type "int", :value 0}
            {:dest "x_3_2", :op "const", :type "int", :value 0}
            {:labels ["l_1_3"], :op "jmp"}])

(def cfg
  '({:name "main",
     :args nil,
     :type nil,
     :instrs
     ({:label :b0,
       :succ [:header],
       :block
       [{:dest "result", :op "const", :type "int", :value 1}
        {:dest "i", :op "const", :type "int", :value 0}],
       :pred []}
      {:label :header,
       :succ [:body :end],
       :block
       [{:label "header"}
        {:dest "zero", :op "const", :type "int", :value 0}
        {:args ["i" "zero"], :dest "cond", :op "gt", :type "bool"}
        {:dest "i", :op "const", :type "int", :value 9},
        {:args ["cond"], :labels ["body" "end"], :op "br"}],
       :pred [:b0 :body]}
      {:label :body,
       :succ [:header],
       :block
       [{:label "body"}
        {:args ["result" "i"], :dest "result", :op "mul", :type "int"}
        {:dest "one", :op "const", :type "int", :value 1}
        {:args ["i" "one"], :dest "i", :op "sub", :type "int"}
        {:labels ["header"], :op "jmp"}],
       :pred [:header]}
      {:label :end,
       :succ [],
       :block [{:label "end"} {:args ["result"], :op "print"}],
       :pred [:header]})}))
