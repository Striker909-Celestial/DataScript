package com.striker.datascript.objects;

import java.util.function.Supplier;

public class ScriptNumber implements ScriptObject<Double> {

    public static final ScriptNumber ZERO = new ScriptNumber(0);
    public static final ScriptNumber ONE = new ScriptNumber(1);

    private final double value;
    private final Supplier<Double> supplier;

    public ScriptNumber(double value) {
        this.value = value;
        this.supplier = () -> this.value;
    }

    public Supplier<Double> supplier() { return supplier; }
    public Double get() { return value; }
    public double comparisonNumber() { return value; }
}
