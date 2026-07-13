package com.striker.datascript.objects;

import java.util.function.Supplier;

public class ScriptBoolean implements ScriptObject<Boolean> {

    public static final ScriptBoolean TRUE = new ScriptBoolean(true);
    public static final ScriptBoolean FALSE = new ScriptBoolean(false);

    private Supplier<Boolean> supplier;

    public ScriptBoolean(boolean value) { this.supplier = () -> value; }
    public ScriptBoolean(Supplier<Boolean> supplier) { this.supplier = supplier; }

    public Supplier<Boolean> supplier() { return supplier; }
    public void setSupplier(Supplier<?> supplier) { this.supplier = () -> (boolean) supplier.get(); }
    public Boolean get() { return supplier.get(); }
    public double comparisonNumber() { return get() ? 1 : 0; }
}
