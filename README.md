# bril-clj

`bril-clj` provides LLVM-like optimizations for
[Bril](https://capra.cs.cornell.edu/bril/intro.html) the Big Red IR
(Bril is a compiler intermediate representation for learning).


## Installation

TODO: Build Instructions.

Download from http://example.com/FIXME.

## Usage

TODO: add `main` function and `args`
FIXME: explanation

    $ java -jar bril-clj-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this compiler accepts.

## Examples
## Optimizations
### Dead Code Elemenation: Double Assignment
Dead Code Elemenation: doubel assignments, this is a *block* level optimization.
It removes unused variable declarations.
```python
  main {
    a: int = const 4;
    a: int = const 4;
    print a;
  }
```
first `a` will be removed.
### Dead Code Elemenation
Dead Code Elemenation: unused variable declarations, this is a *function* level optimization.
It removes unused variable declarations.

  ```python
  main {
    a: int = const 4;
    b: int = const 4;
    print b;
  }

  ```
`a` will be removed.

### Common Subexpression Elemenation
Common expressions that are commutative could be identified, such as: `add a b` is equivalent to `add b a`.

```python
@main {
    a: int = const 4;
    b: int = const 2;
    sum1: int = add a b; 
    sum2: int = add b a;
    prod: int = int = mul sum1 sum2;
    print prod;
}
```
Gets optimized to:
```diff
    sum1: int = add a b; 
-   sum2: int = add b a;
-   prod: int = int = mul sum1 sum2;
+   prod: int = int = mul sum1 sum1;
    print prod;
```

### Copy Propagation

Chains of assignment can directly refer to the original variable.

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
```
With dead code elimination:

```python
@main {
    x: int = const 4;
    copy3: int = id x;
    print copy3;
}
```
Later, with constant folding (TODO) can directly do:

```python
@main {
    print 4; # Amazing!
}
```

### Constant Propagation
Similar to [Constant Propagation](#constant-propagation), but the constant itself propagates (as the name implies!).

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
```

### Constant Folding
All of this can be done at compile time!

```python
@main {
    a:      int = const 4;
    b:      int = const 2; 

    sum1:   int = add a b;
    sum2:   int = add a b;
    prod1: int = mul suml sum2; 

    suml:   int = const 0;
    sum2:   int = const 0; 

    sum3:   int = add a b;
    prod2:  int = Rul sum3 sum3; 

    print prod2; 
} 
```
Is folded to **this only**! Truly magic! Special thanks to the spirit that lives inside the computer!

```python
@main {
    prod2: int = const 36;
    print prod2;
}
```

...

### Bugs

...

## Why Clojure?
> Lisp is for building organisms—imposing, breathtaking, dynamic
> structures built by squads fluctuating myriads of simpler organisms
> into place.  
> -- *Structure and Interpretation of Programs*

[Clojure](https://clojure.org) is a LISP (LIST Processing)
language. It is powerful, simple (arguable), flexible, immutable, and
functional. It is designed for list processing (aka data-first),
perfect for building compiler.

Clojure's syntax is extremely terse, if python can do in 3 lines what
C does in 10, clojure can do in 1.


Perhaps a code example is worth a thousand words. Here we see a data *pipeline*, where the `lvn-table` flows between
different optimizations. This example demonstrates elegant high-order
functions (partial application, or [currying](https://en.wikipedia.org/wiki/Currying).

```clojure
(->> lvn-table
     (map (partial optimize:copy-propagation-lvn-args     lvn-table))
     (map (partial optimize:constant-propagation-lvn-args lvn-table))
     (map (partial optimize:constant-folding              lvn-table)))
```

Isn't this elegant? The code contains plethora of such examples, data
flows cleanly between functions.

To add another example, *constant folding*; relatively not so trivial
in python, could be done in Clojure with the magic of high-order
functions.

```clojure
(defn optimize:constant-folding
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
```

I consider these lines specifically, compared to python:

```clojure
;; This line set op-fn (operator function) 
(let [op-fn ({"add" +, "mul" *, "sub" -, "div" /} (:op (:value lvn-row)))]
;...
  (apply op-fn args))
```

Python Equivalent
```python
;; op_fn = {
;;    "add": lambda a, b: a + b,
;;    "mul": lambda a, b: a * b,
;;    "sub": lambda a, b: a - b,
;;    "div": lambda a, b: a / b
;; }[lvn_row["value"]["op"]]
;;
;; result = op_fn(*args)  # Equivalent to (apply op-fn args)
```
There is no comparison.

Add to that, Clojure has the [best debugging
experience](https://valer.dev/posts/clojure-debugging/) I have ever
seen.

## License

Copyright © 2025 Flinner Yuu

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
