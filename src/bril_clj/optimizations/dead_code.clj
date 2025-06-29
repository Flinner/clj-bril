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
  "
  [block])

  
  


;; Send to
;; echo "$OUTPUT" | dot -Tpng | feh -
