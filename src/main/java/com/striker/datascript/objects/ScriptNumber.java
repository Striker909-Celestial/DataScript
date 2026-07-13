package com.striker.datascript.objects;

import java.util.function.Supplier;

public class ScriptNumber implements ScriptObject<Double> {

    public static final ScriptNumber ZERO = new ScriptNumber(0);
    public static final ScriptNumber ONE = new ScriptNumber(1);

    private Supplier<Double> supplier;

    public ScriptNumber(double value) { this.supplier = () -> value; }
    public ScriptNumber(Supplier<Double> supplier) { this.supplier = supplier; }

    public Supplier<Double> supplier() { return supplier; }
    public void setSupplier(Supplier<?> supplier) { this.supplier = () -> (double) supplier.get(); }
    public Double get() { return supplier.get(); }
    public double comparisonNumber() { return get(); }
}
