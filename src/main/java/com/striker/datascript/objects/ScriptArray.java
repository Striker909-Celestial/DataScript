package com.striker.datascript.objects;

import java.util.List;
import java.util.function.Supplier;

public class ScriptArray implements ScriptObject<List<ScriptObject<?>>> {

    public static final ScriptArray EMPTY = new ScriptArray(List.of());

    private Supplier<List<ScriptObject<?>>> supplier;

    public ScriptArray(List<ScriptObject<?>> value) { this.supplier = () -> value; }

    public Supplier<List<ScriptObject<?>>> supplier() { return supplier; }
    public void setSupplier(Supplier<?> supplier) { this.supplier = () -> (List<ScriptObject<?>>) supplier.get(); }
    public List<ScriptObject<?>> get() { return supplier.get(); }
    public ScriptObject<?> get(int index) { return get().get(index); }
    public int size() { return get().size(); }
    public double comparisonNumber() {
        double num = size();
        for (int i = 0; i < size(); i++) {
            num += 0.01 * (size() - i) * get(i).comparisonNumber();
        }
        return num;
    }
    public String toString() { return get().toString(); }
}
