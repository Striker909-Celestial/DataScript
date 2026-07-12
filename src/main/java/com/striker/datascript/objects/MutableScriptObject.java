package com.striker.datascript.objects;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MutableScriptObject implements ScriptObject<Consumer<ScriptObject<?>>> {

    public static final MutableScriptObject DUMMY = new MutableScriptObject(value -> {});

    private final Consumer<ScriptObject<?>> consumer;
    private final Supplier<Consumer<ScriptObject<?>>> supplier;

    public MutableScriptObject(Consumer<ScriptObject<?>> value) {
        this.consumer = value;
        this.supplier = () -> this.consumer;
    }

    public Supplier<Consumer<ScriptObject<?>>> supplier() { return supplier; }
    public Consumer<ScriptObject<?>> get() { return consumer; }
    public double comparisonNumber() { return 0; }
    public void accept(ScriptObject<?> value) { consumer.accept(value); }
}
