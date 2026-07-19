package com.striker.datascript.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ScriptFunction<T extends ScriptObject<?>> implements ScriptObject<Function<Map<String, ScriptObject<?>>, T>> {

    public static ScriptFunction<ScriptObject<Object>> DUMMY = new ScriptFunction<>();
    public static ScriptFunction<ScriptString> DUMMY_STRING = new ScriptFunction<>(args -> ScriptString.EMPTY, ScriptStructure.EMPTY);
    public static ScriptFunction<ScriptNumber> DUMMY_NUMBER = new ScriptFunction<>(args -> ScriptNumber.ZERO, ScriptStructure.EMPTY);
    public static ScriptFunction<ScriptBoolean> DUMMY_BOOLEAN = new ScriptFunction<>(args -> ScriptBoolean.FALSE, ScriptStructure.EMPTY);
    public static ScriptFunction<ScriptArray> DUMMY_ARRAY = new ScriptFunction<>(args -> ScriptArray.EMPTY, ScriptStructure.EMPTY);
    public static ScriptFunction<ScriptStructure> DUMMY_STRUCTURE = new ScriptFunction<>(args -> ScriptStructure.EMPTY, ScriptStructure.EMPTY);

    private Supplier<Function<Map<String, ScriptObject<?>>, T>> function;
    private final ScriptStructure defaults;
    public final String docs;

    public ScriptFunction(Function<Map<String, ScriptObject<?>>, T> function, ScriptStructure defaults, String docs) {
        this.function = () -> function;
        this.defaults = defaults;
        this.docs = docs;
    }
    public ScriptFunction(Function<Map<String, ScriptObject<?>>, T> function, ScriptStructure defaults, ScriptString docs) {
        this(function, defaults, docs.get().toString());
    }
    public ScriptFunction(Function<Map<String, ScriptObject<?>>, T> function, ScriptStructure defaults) {
        this(function, defaults, "");
    }
    public ScriptFunction() {
        this.function = () -> (args -> null);
        this.defaults = null;
        this.docs = "";
    }

    public ScriptStructure defaults() { return defaults; }

    private Map<String, ScriptObject<?>> applyArgs(Map<String, ScriptObject<?>> args) {
        if (this.defaults == null) { return args; }
        Map<String, ScriptObject<?>> newArgs = new HashMap<>(args);
        for (String key : this.defaults.data().keySet()) {
            if (args.containsKey(key)) {
                newArgs.put(key, args.get(key));
            } else {
                newArgs.put(key, this.defaults.data().get(key));
            }
        }
        return newArgs;
    }

    private Map<String, ScriptObject<?>> applyArgs(ScriptStructure args) {
        return this.applyArgs(args.data());
    }

    private T apply(Map<String, ScriptObject<?>> args) {
        Map<String, ScriptObject<?>> newArgs = this.applyArgs(args);
        return function.get().apply(newArgs);
    }

    public T apply(ScriptStructure args) {
        return this.apply(args.data());
    }

    private T apply(List<ScriptObject<?>> args) {
        if (this.defaults == null) { return null; }
        Map<String, ScriptObject<?>> newArgs = new HashMap<>();
        int i = 0;
        for (String key : defaults.data().keySet()) {
            if (i >= args.size()) { break; }
            newArgs.put(key, args.get(i));
            i++;
        }
        return this.apply(newArgs);
    }

    public T apply(ScriptArray args) {
        return this.apply(args.get());
    }

    public T apply() {
        if (this.defaults == null) { return null; }
        return this.apply(defaults);
    }

    public ScriptFunction<T> updateDefaults(ScriptStructure args) {
        if (this.defaults == null) { return new ScriptFunction<>(function.get(), args); }
        Map<String, ScriptObject<?>> newDefaults = this.applyArgs(args);
        return new ScriptFunction<>(function.get(), new ScriptStructure(newDefaults));
    }

    public ScriptFunction<T> link(MutableScriptObject target) {
        return new ScriptFunction<>(
                args -> {
                    T result = this.apply(args);
                    target.accept(result);
                    return result;
                },
                this.defaults
        );
    }

    public Supplier<Function<Map<String, ScriptObject<?>>, T>> supplier() { return this.function; }
    public void setSupplier(Supplier<?> supplier) { this.function = () -> (Function<Map<String, ScriptObject<?>>, T>) supplier.get(); }
    public Function<Map<String, ScriptObject<?>>, T> get() { return function.get(); }
    public double comparisonNumber() { return 0; }
    public String toString() { return get().toString(); }
}
