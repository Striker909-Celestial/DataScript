## What is DataScript?

DataScript is a minimal functional scripting language based off of canonical structured data files such as TOML, JSON, or YAML. It is written in Java and can interact with and use functions imported from Java. Functions can also be defined and called within data files, along with data objects that are used by functions.

TOML will be used for all DataScript examples in this README, but DataScript can read from any file with a similar "maps in maps" style of hierarchy. 

DataScript is not intended to be used as a standalone language, but instead as a supplement to Java. By itself, it only has basic arithmetic and flow abilities and no mechanism for input or output. It is instead designed to be used as snippets employed by a larger program that can be modified *without* recompilation, allowing for quick iteration or even on-the-fly code generation.

Most importantly, this README is more of a concept layout than an actual description of DataScript's current functionality. As of `v0.2.4` on July 20th, 2026, the code has many bugs and is still incomplete, although it does have some basic functionality. Additionally, DataScript is largely just a project to push my coding skills to their limits and prove to myself (and the computer science department at my college) that I am ready to move on to more advance topics.

## DataScript Objects

Data objects are the most straight-forward element of DataScript. On compilation, all values from key-value pairs are converted into a `ScriptObject` corresponding to their type. Integers and floats are converted to `ScriptNumber`, booleans are converted to `ScriptBoolean`, and lists are converted into `ScriptArray`. These are the "simple" objects that behave exactly as you would expect they would. The only slight wrinkle is that each element within a list will also be converted to a `ScriptObject` accordingly. Lists with different types of elements are supported.

```toml
[data.ex1]
num1 = 0.5                    # ScriptNumber(0.5)
num2 = 7                      # ScriptNumber(7.0)
bool = true                   # ScriptBoolean(true)
array = [1, [true, false]]    # ScriptArray(ScriptNumber(1), ScriptArray(ScriptBoolean(true), ScriptBoolean(false)))
```

### Structures

Structures are DataScript's equivalent of maps. As their name implies, they make up the entire structure of code in DataScript. While they can simply act as a map of other data types (or other structures), they also are used both for function calls and function definition, which will be described in more detail in a later section.

### Strings

Two types are notably omitted from the "simple" category: Strings and Maps. Both are the driving forces behind what elevates DataScript from a file reader to an actual scripting language. Maps will be covered in a later section.

Strings serve three main purposes: they can store text, but they can also reference other pieces of data and perform calculations.

#### Text Strings

Text strings do what their name implies: they store text. The only real difference from strings in a normal file is that `$`, `@`, `{`, and `}` are considered special characters and thus must be escaped like `\\$`.

#### Formatted Strings

Formatted strings are a variation on text strings that allow for the insertion of references to data elsewhere in the file. If any section of a string is enclosed with unescaped curly braces, the section within will be treated as a separate string of any type. Its value will be converted back into a string and inserted into the original string.

```toml
[data.ex2]
num = 3
array = [1, 2, 3]
string = "Num is {$num}. Array is {$array}." # ScriptString("Num is 3. Array is [1, 2, 3]")
```

Formatted strings can be put within formatted strings. If the value of an insert to a formatted sting changes, the string will also change accordingly.

#### References

References allow different sections of code to share data. They are also the mechanism by which functions are used. A reference can be thought of as creating a local pointer to a certain object. If that object changes, the reference will also change. 

References are created with strings in a certain pattern, similar to an address in a file system. A prefix (`$` or `@`) starts a reference. 

- `$` will be used in most cases. It indicates that the reference is read-only, meaning if `b` is a reference to `a`, when `a` changes it will cause `b` to change, but changing `b` will not affect `a`.
- `@` starts a write-only reference. This is not a reference to a value, but instead a mutator for that value. The `@` prefix is most commonly used with the `link` command.

After the prefix is a chain of directions to the value that is being referenced. This is best explained through example:

```toml
[data.ex3]
value = 3
ref = "$value"
```

As `ref` and `value` have the same scope, `ref` can reference `value` directly.

```toml
[data.ex4]
a = { b = { c = 3 } }
d = { e = { f = "$a.b.c" } }
```

As `c` and `f`'s scope are separated, `f` must start the reference from the lowest shared scope, in this case `data.ex4`, and then move down scopes until it reaches `c`'s scope.

Items in arrays can also be referenced by their indexes. For example, `$array.1` would reference the first item of the array. If an item was inserted at position 1, the reference would not switch to referencing that item, it would maintain its original reference even as the item referenced moves to index 2.

### Function Calls

A function – be it a core function, an imported function, or a function defined in-file – is called through a specific structure that includes a `$` reference to the function being called and a map or array with arguments. It will loom something like:

```toml
[data.ex5]
function_call_resullt = {
    run.func = "$range",
    run.args = {
        start = 10,
        end = 0,
        step = -1
    }
}
```

The `run` structure is what triggers the function call. Without that keyword, this would merely act as a data structure. When the run structure is detected, it will search for a `func` child. If one is not found, an error will be thrown.

Arguments can be passed through a map or omitted entirely. All functions have default values for each of their arguments. Any arguments passed will override the corresponding default. If no arguments are passed, the function will be called with default parameters. 

It is important to note that just like how references update when the data they point to updates, the output of a function call will change if any items its `args` references or its `func` changes.

#### Last updated July 20th, 2026 for v0.2.4

TODO: Core Functions, Function Definition, Imports, Imports from Java