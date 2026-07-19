package com.striker.datascript.objects;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MutableScriptObject implements ScriptObject<Consumer<ScriptObject<?>>> {

    public static final MutableScriptObject DUMMY = new MutableScriptObject(() -> (value -> {}));

    private Supplier<Consumer<ScriptObject<?>>> supplier;

    public MutableScriptObject(Supplier<Consumer<ScriptObject<?>>> supplier) { this.supplier = supplier; }
    public MutableScriptObject(Consumer<ScriptObject<?>> consumer) { this.supplier = () -> consumer; }

    public Supplier<Consumer<ScriptObject<?>>> supplier() { return supplier; }
    public void setSupplier(Supplier<?> supplier) { this.supplier = () -> (Consumer<ScriptObject<?>>) supplier.get(); }
    public Consumer<ScriptObject<?>> get() { return supplier.get(); }
    public double comparisonNumber() { return 0; }
    public void accept(ScriptObject<?> value) { supplier.get().accept(value); }
    public String toString() { return get().toString(); }
}
