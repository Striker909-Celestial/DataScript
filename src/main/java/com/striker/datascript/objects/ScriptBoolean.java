package com.striker.datascript.objects;

import java.util.function.Supplier;

public class ScriptBoolean implements ScriptObject<Boolean> {

    public static final ScriptBoolean TRUE = new ScriptBoolean(true);
    public static final ScriptBoolean FALSE = new ScriptBoolean(false);

    private final boolean value;
    private final Supplier<Boolean> supplier;

    public ScriptBoolean(boolean value) {
        this.value = value;
        this.supplier = () -> this.value;
    }

    public Supplier<Boolean> supplier() { return supplier; }
    public Boolean get() { return value; }
    public double comparisonNumber() { return value ? 1 : 0; }
}
