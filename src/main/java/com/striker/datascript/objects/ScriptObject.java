package com.striker.datascript.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface ScriptObject<T> {

    public static ScriptObject<?> DUMMY = new ScriptObject<Object>() {
        @Override
        public Supplier<Object> supplier() { return () -> null; }
        @Override
        public void setSupplier(Supplier<?> supplier) {}
        @Override
        public Object get() { return null; }
        @Override
        public double comparisonNumber() { return 0; }
    };

    Supplier<T> supplier();
    void setSupplier(Supplier<?> supplier);
    T get();
    double comparisonNumber();

    static ScriptObject<?> of(Object value) {
        return switch (value) {
            case ScriptObject<?> obj -> obj;
            case Supplier<?> supplier -> {
                ScriptObject<?> output = of(supplier.get());
                assert output != null;
                output.setSupplier(supplier);
                yield output;
            }
            case String string -> new ScriptString(string);
            case Number number -> new ScriptNumber(number.doubleValue());
            case Boolean bool -> new ScriptBoolean(bool);
            case List<?> list -> {
                List<ScriptObject<?>> newList = new ArrayList<>();
                for (var item : list) {
                    newList.add(of(item));
                }
                yield new ScriptArray(newList);
            }
            case Map<?, ?> map -> {
                Map<String, ScriptObject<?>> newMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    newMap.put(entry.getKey().toString(), of(entry.getValue()));
                }
                yield new ScriptStructure(newMap);
            }
            default -> null;
        };
    }

    static <T extends ScriptObject<?>> boolean matchType(ScriptObject<?> value, T reference) { return value.getClass() == reference.getClass(); }

    static <T extends ScriptObject<?>> T assertType(ScriptObject<?> value, T fallback) {
        if (matchType(value, fallback)) { return (T) value; }
        return fallback;
    }
}
