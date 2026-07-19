package com.striker.datascript.objects;

import com.striker.datascript.Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/// A ScriptStructure is the core building block of a DataScript program. At its most basic level, a ScriptStrucure acts as a map of other [ScriptObject]s.
/// However, a ScriptStructure can also act as an import block, a function definition, or a function call.
///
/// @author Striker-909
/// @since v0.1.0
public class ScriptStructure implements ScriptObject<Object> {

    /// An empty ScriptStructure that can serve as a placeholder.
    public static final ScriptStructure EMPTY = new ScriptStructure();

    private enum Type {
        FUNCTION_CALL,
        FUNCTION,
        DATA,
        IMPORT,
    }

    private final String path;
    private final Function<String, Supplier<ScriptObject<?>>> context;

    private Type type = Type.DATA;
    private Supplier<Map<String, ScriptObject<?>>> dataSupplier;
    private Supplier<Map<String, ScriptObject<?>>> argsSupplier;
    private Supplier<?> supplier;

    /// A ScriptStructure is the core building block of a DataScript program. At its most basic level, a ScriptStrucure acts as a map of other [ScriptObject]s.
    /// However, a ScriptStructure can also act as an import block, a function definition, or a function call.
    ///
    /// This initializer accepts a map of raw Java objects that are then converted into ScriptObjects.
    /// If any of the objects are maps, a ScriptStructure will be created from it in a recursive manner.
    ///
    /// @param path The path to this ScriptStructure from its root, i.e., the file it is initialized in.
    /// @param data A map of data and the keys associated with that data.
    /// @param context A function that accepts a path and returns a supplier for the variable associated with that path.
    public ScriptStructure(String path, Map<String, Object> data, Function<String, Supplier<ScriptObject<?>>> context) {
        HashMap<String, ScriptObject<?>> dataMap = new HashMap<>();
        this.supplier = this.dataSupplier = () -> dataMap;
        this.path = path;
        // builds the contex for this structure
        this.context = (reference) -> {
            Supplier<ScriptObject<?>> supplier = Core.context(reference);
            if (supplier != null) { return supplier; }
            return context.apply(reference);
        };

        String[] splitPath = path.split("\\.");
        if (splitPath.length >= 1 && splitPath[splitPath.length - 1].equals("import")) { // if the path ends with import, this is an import structure
            this.type = Type.IMPORT;
            //TODO: Implement imports
        } else if (splitPath.length >= 2 && splitPath[splitPath.length - 2].equals("function")) { // if the path looks like "function.name", this is a function structure
            this.type = Type.FUNCTION;
        }

        // the new context that is passed to script structures and strings that are children of this structure
        Function<String, Supplier<ScriptObject<?>>> childContext = (reference) -> {
            Supplier<ScriptObject<?>> supplier = this.supplier(reference);
            if (supplier != null) { return supplier; }
            return context.apply(reference);
        };
        // loops through all entries in the input data
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            ScriptObject<?> value = switch (entry.getValue()) {
                case String s -> {
                    ScriptString scriptString = new ScriptString(s, childContext);
                    if (scriptString.isText()) { yield scriptString; }
                    yield ScriptObject.of(scriptString.get());
                }
                case Map m -> new ScriptStructure(path + "." + entry.getKey(), m, childContext); // maps become script structures with context
                default -> ScriptObject.of(entry.getValue()); // all other objects become script objects with no context
            };
            if (value == null) { continue; } // skips if the entry could not be converted into a script object
            dataMap.put(entry.getKey(), value);
        }

        if (this.type == Type.FUNCTION) {
            // defaults come from the args child structure if it exists
            ScriptStructure defaults = this.data().containsKey("args") ? ScriptObject.assertType(this.get("$args"), ScriptStructure.EMPTY) : ScriptStructure.EMPTY;
            Function<Map<String, ScriptObject<?>>, ScriptObject<?>> function = (args) -> {
                argsSupplier = () -> args;
                return this.supplier("return").get();
            };
            String docs = this.data().containsKey("docs") ? this.get("$docs").get().toString() : "";
            ScriptFunction<?> scriptFunction = new ScriptFunction<>(function, defaults, docs);
            this.supplier = () -> scriptFunction;
        } else if (this.type == Type.DATA && dataMap.containsKey("run")) { // if a "run" child exists, this is a function call structure
            this.type = Type.FUNCTION_CALL;
            // the supplier will now supply the output of the function call
            this.supplier = () -> {
                ScriptObject<?> func = this.get("$run.func");
                if (!(func instanceof ScriptFunction<?>)) {
                    throw new IllegalArgumentException("Function call structure must have a function as its 'func' child");
                }
                ScriptFunction<?> scriptFunction = ScriptObject.assertType(func, ScriptFunction.DUMMY);
                ScriptStructure args = ScriptObject.assertType(this.get("$run.args"), ScriptStructure.EMPTY);
                return scriptFunction.apply(args);
            };
        }
        this.dataSupplier = () -> dataMap;
    }

    /// A simple ScriptObject equivalent for a map linking keys to ScriptObjects.
    ///
    /// This initializer has no path or context.
    ///
    /// @param data The map for this ScriptStructure to represent.
    public ScriptStructure(Map<String, ScriptObject<?>> data) {
        this.supplier = this.dataSupplier = () -> data;
        this.path = "";
        this.context = (reference) -> null;
    }

    /// An initializer for an empty instance of a ScriptStructure.
    private ScriptStructure() { this(new HashMap<>()); }

    /// @return The path to this ScriptStructure from its root, i.e., the file it is initialized in.
    public String path() { return this.path; }
    /// @return A snapshot of the internal map of this ScriptStructure linking keys to ScriptObjects.
    public Map<String, ScriptObject<?>> data() { return this.dataSupplier.get(); }

    ///
    public Supplier<ScriptObject<?>> supplier(String reference) {
        if (reference == null || reference.isBlank()) { return null; }

        char prefix = reference.charAt(0);
        if (prefix != '$' && prefix != '@') {
            reference = "$" + reference;
            prefix = '$';
        }

        if (prefix == '$' && reference.length() == 1) {
            return switch (this.type) {
                case FUNCTION_CALL -> () -> ScriptObject.of(this.supplier);
                default -> () -> this;
            };
        } // If reference is just "$", return the structure itself

        String[] split = reference.substring(1).split("\\.");
        if (this.type == Type.FUNCTION && this.argsSupplier != null && argsSupplier.get().containsKey(split[0])) {
            return () -> argsSupplier.get().get(split[0]);
        }

        if (!this.data().containsKey(split[0])) { return null; } // If reference doesn't exist, return null

        var obj = this.data().get(split[0]);
        if (obj instanceof ScriptStructure structure) { // Allows for indexing into structures such as "$data.structure.key" regardless of reference type
            if (prefix == '@' && split.length == 1) {
                MutableScriptObject mut = getSetter(split[0]);
                return () -> mut;
            }
            if (split.length == 1) { return () -> structure; }
            return structure.supplier(prefix + reference.substring(split[0].length() + 2));
        }

        if (prefix == '$') { // Read-only reference
            return switch (obj) {
                // Allows for indexing into arrays such as "$data.array.0"
                case ScriptArray array -> (split.length > 1 && Pattern.matches("\\d+", split[1])) ? () -> array.get(Integer.parseInt(split[1])) : () -> array;
                default -> () -> obj;
            };
        } else { // Write-only reference
            MutableScriptObject setterObject = getSetter(split[0]);
            return () -> setterObject;
        }
    }

    private MutableScriptObject getSetter(String reference) {
        Consumer<ScriptObject<?>> setter = (o) -> this.data().get(reference).setSupplier(o.supplier());
        return new MutableScriptObject(setter);
    }

    public ScriptObject<?> get(String reference) {
        Supplier<ScriptObject<?>> resolvedSupplier = this.supplier(reference);
        if (resolvedSupplier == null) {
            throw new IllegalArgumentException("Unknown ScriptStructure reference '" + reference + "' in structure '" + this.path + "'");
        }
        return resolvedSupplier.get();
    }

    public Supplier<Object> supplier() { return supplier::get; }
    public void setSupplier(Supplier<?> supplier) { this.supplier = supplier; }
    public Object get() { return supplier == null ? null : supplier.get(); }
    
    public ScriptArray matches(ScriptObject<?> target, ScriptFunction<ScriptBoolean> matcher) {
        ArrayList<ScriptObject<?>> matches = new ArrayList<>();
        for (Map.Entry<String, ScriptObject<?>> entry : this.data().entrySet()) {
            if (matcher.apply(new ScriptArray(List.of(target, new ScriptString(entry.getKey())))).get()) {
                matches.add(entry.getValue());
            }
        }
        return new ScriptArray(matches);
    }

    public int size() { return this.data().size(); }
    public double comparisonNumber() {
        double num = size();
        int i = size();
        for (Map.Entry<String, ScriptObject<?>> entry : this.data().entrySet()) {
            num += 0.01 * i * entry.getValue().comparisonNumber();
            i--;
        }
        return num;
    }

    public String toString() { return String.valueOf(this.get()); }
}
