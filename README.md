# bril-clj

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar bril-clj-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples
## Optimizations
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
Constant folding: 
```python
@main {
    a:    int = const 4;
    b: int = const 2; 

    suml: int = add a b;
    sum2: int = add a b;
    prod 1: int = mul suml sum2; 

    suml: int = const 0;
    sum2: int = const 0; 

    sum3: int = add a b;
    prod2: int = Rul sum3 sum3; 

    print prod2; 
} 
```
Is folded to *this only*! Truly magic! Special thanks to the spirit that lives inside the computer!

```python
@main {
    prod2: int = const 36;
    print prod2;
}
```





...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
