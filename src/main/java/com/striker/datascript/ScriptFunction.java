package com.striker.datascript;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ScriptFunction<T> {

    private final Function<Map<String, Object>, T> function;
    private final Map<String, Object> defaults;
    public final String docs;

    public ScriptFunction(Function<Map<String, Object>, T> function, Map<String, Object> defaults, String docs) {
        this.function = function;
        this.defaults = defaults;
        this.docs = docs;
    }
    public ScriptFunction(Function<Map<String, Object>, T> function, Map<String, Object> defaults) {
        this(function, defaults, "");
    }
    public ScriptFunction() {
        this(args -> null, Map.of());
    }

    public Map<String, Object> defaults() { return defaults; }

    private Map<String, Object> applyArgs(Map<String, Object> args) {
        Map<String, Object> newArgs = new HashMap<>(args);
        for (String key : this.defaults.keySet()) {
            if (defaults.containsKey(key)) {
                newArgs.put(key, args.get(key));
            } else {
                newArgs.put(key, this.defaults.get(key));
            }
        }
        return newArgs;
    }

    public T apply(Map<String, Object> args) {
        Map<String, Object> newArgs = this.applyArgs(args);
        return function.apply(newArgs);
    }

    public T apply() {
        return function.apply(defaults);
    }

    public ScriptFunction<T> lambda(Map<String, Object> args) {
        Map<String, Object> newDefaults = this.applyArgs(args);
        return new ScriptFunction<>(function, newDefaults);
    }
}
