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

public class ScriptStructure implements ScriptObject<Object> {

    public static final ScriptStructure EMPTY = new ScriptStructure();

    private enum Type {
        FUNCTION_CALL,
        FUNCTION,
        DATA,
        IMPORT,
    }

    private final String path;
    private final Function<String, Supplier<ScriptObject<?>>> context;
    private final HashMap<String, ScriptObject<?>> data = new HashMap<>();

    private Type type = Type.DATA;
    private Supplier<?> supplier = () -> this.data;

    public ScriptStructure(String path, Map<String, Object> data, Function<String, Supplier<ScriptObject<?>>> context) {
        this.path = path;
        this.context = (reference) -> {
            Supplier<ScriptObject<?>> supplier = Core.context(reference);
            if (supplier != null) { return supplier; }
            return context.apply(reference);
        };

        Function<String, Supplier<ScriptObject<?>>> newContext = (reference) -> {
            Supplier<ScriptObject<?>> supplier = this.getSupplier(reference);
            if (supplier != null) { return supplier; }
            return context.apply(reference);
        };
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            ScriptObject<?> value = switch (entry.getValue()) {
                case String s -> new ScriptString(s, this.context);
                case Map m -> new ScriptStructure(path + "." + entry.getKey(), m, newContext);
                default -> ScriptObject.of(entry.getValue());
            };
            if (value == null) { continue; }
            this.data.put(entry.getKey(), value);
        }

        String[] splitPath = path.split("\\.");
        if (splitPath[splitPath.length - 1].equals("import")) {
            this.type = Type.IMPORT;
            //TODO: Implement imports
        } else if (splitPath[splitPath.length - 2].equals("function")) {
            this.type = Type.FUNCTION;
            //TODO: Implement function creation
        } else if (this.data.containsKey("run")) {
            this.type = Type.FUNCTION_CALL;
            this.supplier = () -> {
                ScriptFunction<?> scriptFunction = ScriptObject.assertType(context.apply(this.get("$run.func").get().toString()).get(), ScriptFunction.DUMMY);
                ScriptStructure args = ScriptObject.assertType(this.get("$run.args"), ScriptStructure.EMPTY);
                return scriptFunction.apply(args);
            };
        }
    }

    public ScriptStructure(Map<String, ScriptObject<?>> data) {
        this.data.putAll(data);
        this.path = "";
        this.context = (reference) -> null;
    }

    public ScriptStructure() { this(new HashMap<>()); }

    public String path() { return this.path; }

    public Supplier<ScriptObject<?>> getSupplier(String reference) {
        char prefix = reference.charAt(0);
        if (prefix == '$' && reference.length() == 1) { return () -> this; }
        String[] split = reference.substring(1).split("\\.");
        if (!this.data.containsKey(split[0])) { return null; }
        var obj = this.data.get(split[0]);
        if (prefix == '$') {
            return switch (obj) {
                case ScriptArray array -> (split.length > 1 && Pattern.matches("\\d+", split[1])) ? () -> array.get(Integer.parseInt(split[1])) : () -> array;
                case ScriptStructure structure -> structure.getSupplier(prefix + reference.substring(split[0].length() + 1));
                default -> () -> obj;
            };
        } else if (prefix == '@') {
            MutableScriptObject setterObject = getSetter(new ScriptString(split[0]));
            return () -> setterObject;
        }
        return null;
    }

    private MutableScriptObject getSetter(ScriptString reference) {
        String ref = reference.get().toString();
        Consumer<ScriptObject<?>> setter = (o) -> this.data.put(ref, o);
        return new MutableScriptObject(setter);
    }

    public ScriptObject<?> get(String reference) {
        return this.getSupplier(reference).get();
    }

    public Supplier<Object> supplier() { return supplier::get; }
    public Object get() { return supplier.get(); }
    public Map<String, ScriptObject<?>> data() { return this.data; }
    
    public ScriptArray matches(ScriptObject<?> target, ScriptFunction<ScriptBoolean> matcher) {
        ArrayList<ScriptObject<?>> matches = new ArrayList<>();
        for (Map.Entry<String, ScriptObject<?>> entry : this.data.entrySet()) {
            if (matcher.apply(new ScriptArray(List.of(target, new ScriptString(entry.getKey())))).get()) {
                matches.add(entry.getValue());
            }
        }
        return new ScriptArray(matches);
    }

    public int size() { return this.data.size(); }
    public double comparisonNumber() {
        double num = size();
        int i = size();
        for (Map.Entry<String, ScriptObject<?>> entry : this.data.entrySet()) {
            num += 0.01 * i * entry.getValue().comparisonNumber();
            i--;
        }
        return num;
    }
}
