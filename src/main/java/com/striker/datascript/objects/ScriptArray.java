package com.striker.datascript.objects;

import java.util.List;
import java.util.function.Supplier;

public class ScriptArray implements ScriptObject<List<ScriptObject<?>>> {

    public static final ScriptArray EMPTY = new ScriptArray(List.of());

    private final List<ScriptObject<?>> array;
    private final Supplier<List<ScriptObject<?>>> supplier;

    public ScriptArray(List<ScriptObject<?>> value) {
        this.array = value;
        this.supplier = () -> this.array;
    }

    public Supplier<List<ScriptObject<?>>> supplier() { return supplier; }
    public List<ScriptObject<?>> get() { return array; }
    public ScriptObject<?> get(int index) { return array.get(index); }
    public int size() { return array.size(); }
    public double comparisonNumber() {
        double num = size();
        for (int i = 0; i < size(); i++) {
            num += 0.01 * (size() - i) * get(i).comparisonNumber();
        }
        return num;
    }
}
