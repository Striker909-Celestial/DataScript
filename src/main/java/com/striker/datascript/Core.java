package com.striker.datascript;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Core {

    /// Runs the given [ScriptFunction] with its default args.
    public static <T> T run(ScriptFunction<T> func) {
        return func.apply();
    }
    /// Runs the given [ScriptFunction] with the given [Map] of arguments.
    public static <T> T run(ScriptFunction<T> func, Map<String, Object> args) {
        return func.apply(args);
    }
    /// Runs the given [ScriptFunction] with the given [List] of arguments.
    public static <T> T run(ScriptFunction<T> func, List<Object> args) {
        Map<String, Object> new_args = new HashMap<>();
        int i = 0;
        for (String key : func.defaults().keySet()) {
            if (i >= args.size()) { break; }
            new_args.put(key, args.get(i));
            i++;
        }
        return run(func, new_args);
    }

    /// Transforms a [ScriptFunction] by overriding its defaults with the given args.
    /// If any args are ommitted, the corresponding orginal defaults will be preserved.
    public static <T> ScriptFunction<T> lambda(ScriptFunction<T> func, Map<String, Object> args) {
        return func.lambda(args);
    }

    /// Returns a [ScriptFunction] that sends the output of the given [ScriptFunction] to the given [Consumer].
    public static <T> ScriptFunction<T> link(Consumer<T> target,ScriptFunction<T> func) {
        return new ScriptFunction<>(
                args -> {
                    T result = run(func, args);
                    target.accept(result);
                    return result;
                },
                func.defaults());
    }

    /// Attempts to cast the given value to the given type, returns null if the cast fails.
    public static <T> Object cast(String type, T value) {
        try {
            return switch (type.toLowerCase()) {
                case "int" -> (int) value;
                case "float" -> (float) value;
                case "str", "string" -> (String) value;
                case "bool", "boolean" -> (boolean) value;
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    /// Checks if the given condition is true.
    /// If it is, the then [ScriptFunction] will be run with its default parameters and the output returned.
    /// If not, the else [ScriptFunction] will be run with its default parameters and the output returned.
    public static <T, U> Object _if(boolean condition, ScriptFunction<T> then, ScriptFunction<U> _else) {
        if (condition) {
            return run(then);
        }
        return run(_else);
    }

    /// Uses the matcher to check for the first key in the cases that matches the given value.
    /// Once the matching key is found, runs with default parameters and returns the output of the corresponding [ScriptFunction].
    /// If no keys match, the default [ScriptFunction] is run with its default parameters instead.
    public static Object _switch(Object value, ScriptFunction<Boolean> matcher, Map<Object, ScriptFunction<?>> cases, ScriptFunction<?> _default) {
        for (Map.Entry<Object, ScriptFunction<?>> entry : cases.entrySet()) {
            if (run(matcher, List.of(value, entry.getKey())) == true) {
                return run(entry.getValue());
            }
        }
        return run(_default);
    }

    /// Runs a while loop, each loop running the condition [ScriptFunction] and then running the func if the condition is true.
    /// Adds the output of the func each loop to an array, which is returned once the loop finishes.
    public static <T> List<T> _while(ScriptFunction<Boolean> condition, ScriptFunction<T> func) {
        List<T> results = new ArrayList<>();
        while (run(condition)) {
            results.add(run(func));
        }
        return results;
    }

    /// Loops through an array or a map, applies the func [ScriptFunction] to the value at each index.
    /// The output of the func is shaved to an array and returned.
    /// The items from the iterable are passed to the func with the val_kw given as the keyword.
    public static <I extends Iterable<T>, T> List<T> foreach(I iterable, String val_kw, ScriptFunction<T> func) {
        List<T> results = new ArrayList<>();
        for (T item : iterable) {
            results.add(run(func, Map.of(val_kw, item)));
        }
        return results;
    }
    /// Loops through a map, applies the func [ScriptFunction] to teh key and value at each index.
    /// The output of the func is saved to a map, indexed with the corresponding key.
    /// The keys from the map are passed to the func with the key_kw given as the keyword, and the values are passed with val_kw.
    public static <V, U> Map<String, U> foreach(Map<String, V> iterable, String key_kw, String val_kw, ScriptFunction<U> func) {
        Map<String, U> results = new HashMap<>();
        for (Map.Entry<String, V> item : iterable.entrySet()) {
            results.put(item.getKey(), run(func, Map.of(key_kw, item.getKey(), val_kw, item.getValue())));
        }
        return results;
    }

    /// Generates a list of numbers with the first number being start and end being an exclusive upper bound.
    /// Each number is equal to the previous number plus the step.
    public static List<Double> range(double start, double end, double step) {
        List<Double> results = new ArrayList<>();
        for (double i = start; i < end; i += step) {
            results.add(i);
        }
        return results;
    }

    /// Returns the length of the given array or map.
    public static <I extends Iterable<T>, T> int len(I iterable) {
        if (iterable instanceof List<?>) { return ((List<?>) iterable).size(); }
        if (iterable instanceof Map<?,?>) { return ((Map<?, ?>) iterable).size(); }
        int i = 0;
        for (T _ : iterable) { i++; }
        return i;
    }

    /// Checks if the given target is present in the given array.
    /// Uses the given matcher [ScriptFunction] to determine if an item matches the target.
    public static <I extends Iterable<T>, T> boolean in(I iterable, T target, ScriptFunction<Boolean> matcher) {
        for (T item : iterable) {
            if (run(matcher, List.of(target, item))) { return true; }
        }
        return false;
    }
    /// Checks if the given target is present in the values of the given map.
    /// Uses the given matcher [ScriptFunction] to determine if an item matches the target.
    public static <T> boolean in(Map<String, T> iterable, T target, ScriptFunction<Boolean> matcher) {
        return(in(iterable.values(), target, matcher));
    }
    /// Checks if the given target is a key in the given map.
    public static <T> boolean in(Map<String, T> iterable, String target) {
        return iterable.containsKey(target);
    }

    private static final Consumer<Object> DUMMY_CONSUMER = (Object o) -> {};
    private static final ScriptFunction<Boolean> HASH_MATCHER = new ScriptFunction<>(args -> args.get("a").hashCode() == args.get("b").hashCode(), Map.of("a", 0, "b", 0));

    public static final Map<String, ScriptFunction<?>> CORE = Map.ofEntries(
            Map.entry("lambda", new ScriptFunction<ScriptFunction<?>>(
                    args -> lambda((ScriptFunction) args.get("func"), (Map) args.get("args")),
                    Map.of("func", new ScriptFunction<>(), "args", Map.of())
            )),
            Map.entry("link", new ScriptFunction<ScriptFunction<?>>(
                    args -> link((Consumer) args.get("target"), (ScriptFunction) args.get("func")),
                    Map.of("target", DUMMY_CONSUMER, "func", new ScriptFunction<>())
            )),
            Map.entry("cast", new ScriptFunction<>(
                    args -> cast((String) args.get("type"), args.get("value")),
                    Map.of("type", "string", "value", "")
            )),
            Map.entry("if", new ScriptFunction<>(
                    args -> _if((boolean) args.get("condition"), (ScriptFunction) args.get("then"), (ScriptFunction) args.get("else")),
                    Map.of("condition", false, "then", new ScriptFunction<>(), "else", new ScriptFunction<>())
            )),
            Map.entry("switch", new ScriptFunction<>(
                    args -> _switch(args.get("value"), (ScriptFunction) args.get("matcher"), (Map<Object, ScriptFunction<?>>) args.get("cases"), (ScriptFunction) args.get("default")),
                    Map.of("value", "", "matcher", HASH_MATCHER, "cases", Map.of(), "default", new ScriptFunction<>())
            )),
            Map.entry("while", new ScriptFunction<>(
                    args -> _while((ScriptFunction<Boolean>) args.get("condition"), (ScriptFunction) args.get("func")),
                    Map.of("condition", new ScriptFunction<Boolean>(args -> false, Map.of()), "func", new ScriptFunction<>())
            )),
            Map.entry("foreach", new ScriptFunction<>(
                    args -> {
                        if (!args.get("key_kw").equals("")) { return foreach((Map) args.get("iterable"), (String) args.get("key_kw"), (String) args.get("val_kw"), (ScriptFunction) args.get("func")); }
                        return foreach((Iterable) args.get("iterable"), (String) args.get("val_kw"), (ScriptFunction) args.get("func"));
                    },
                    Map.of("iterable", new ArrayList<>(), "key_kw", "", "val_kw", "", "func", new ScriptFunction<>())
            )),
            Map.entry("range", new ScriptFunction<>(
                    args -> range((double) args.get("start"), (double) args.get("end"), (double) args.get("step")),
                    Map.of("start", 0.0, "end", 0.0, "step", 1.0)
            )),
            Map.entry("len", new ScriptFunction<>(
                    args -> len((Iterable) args.get("iterable")),
                    Map.of("iterable", new ArrayList<>())
            )),
            Map.entry("in", new ScriptFunction<>(
                    args -> {
                        if (!(args.get("iterable") instanceof Map)) { return in((Iterable) args.get("iterable"), args.get("target"), (ScriptFunction) args.get("matcher")); }
                        if (args.get("target") instanceof String) { return in((Map) args.get("iterable"), (String) args.get("target")); }
                        return in((Map) args.get("iterable"), args.get("target"), (ScriptFunction) args.get("matcher"));
                    },
                    Map.of("iterable", new ArrayList<>(), "target", "", "matcher", HASH_MATCHER)
            ))
    );

    public static final Map<String, ScriptFunction<?>> OPERATORS = Map.ofEntries(
            Map.entry("+", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return (double) a + (double) b; }
                        if (a instanceof List) {
                            if (b instanceof List) { ((List) a).addAll((List) b); }
                            else { ((List) a).add(b); };
                            return a;
                        }
                        if (b instanceof List) {
                            ((List) b).addFirst(a);
                            return b;
                        }
                        if (a instanceof Map && b instanceof Map) {
                            ((Map) a).putAll((Map) b);
                            return a;
                        }
                        return a.toString() + b.toString();
                    },
                    Map.of("a", 0, "b", 0)
            )),
            Map.entry("-", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return (double) a - (double) b; }
                        if (a instanceof String && b instanceof String) { return ((String) a).replace((String) b, ""); }
                        if (a instanceof Map && b instanceof String) { return ((Map) a).remove(b); }
                        if (a instanceof List && b instanceof Number) { return ((List) a).remove((int) b); }
                        return a.hashCode() - b.hashCode();
                    },
                    Map.of("a", 0, "b", 0)
            )),
            Map.entry("*", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return (double) a * (double) b; }
                        if (a instanceof String && b instanceof Integer) { return ((String) a).repeat((int) b); }
                        if (a instanceof List && b instanceof Integer) {
                            List<Object> newList = new ArrayList<>();
                            for (int i = 0; i < (int) b; i++) { newList.addAll((List) a); }
                            return newList;
                        }
                        return a.hashCode() * b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("/", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return (double) a / (double) b; }
                        return a.hashCode() / b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("%", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return (double) a % (double) b; }
                        return a.hashCode() % b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("**", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Number && b instanceof Number) { return Math.pow((double) a,  (double) b); }
                        return Math.pow(a.hashCode(), b.hashCode());
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("==", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        return a.equals(b);
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("!=", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        return !a.equals(b);
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry(">", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Comparable && b instanceof Comparable) { return ((Comparable) a).compareTo(b) > 0; }
                        if (a instanceof Number && b instanceof Number) { return (double) a > (double) b; }
                        return a.hashCode() > b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("<", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Comparable && b instanceof Comparable) { return ((Comparable) a).compareTo(b) < 0; }
                        if (a instanceof Number && b instanceof Number) { return (double) a < (double) b; }
                        return a.hashCode() < b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry(">=", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Comparable && b instanceof Comparable) { return ((Comparable) a).compareTo(b) >= 0; }
                        if (a instanceof Number && b instanceof Number) { return (double) a >= (double) b; }
                        return a.hashCode() >= b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("<=", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Comparable && b instanceof Comparable) { return ((Comparable) a).compareTo(b) <= 0; }
                        if (a instanceof Number && b instanceof Number) { return (double) a <= (double) b; }
                        return a.hashCode() <= b.hashCode();
                    },
                    Map.of("a", 1, "b", 1)
            )),
            Map.entry("&&", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Boolean && b instanceof Boolean) { return (boolean) a && (boolean) b; }
                        return false;
                    },
                    Map.of("a", false, "b", false)
            )),
            Map.entry("||", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        var b = args.get("b");
                        if (a instanceof Boolean && b instanceof Boolean) { return (boolean) a || (boolean) b; }
                        return false;
                    },
                    Map.of("a", false, "b", false)
            )),
            Map.entry("!", new ScriptFunction<>(
                    args -> {
                        var a = args.get("a");
                        if (a instanceof Boolean) { return !(boolean) a ; }
                        return false;
                    },
                    Map.of("a", false)
            ))
    );
    
    public static final LinkedHashMap<String, Pattern> OPERATOR_PATTERNS = Stream.of(
            Map.entry("+",   Pattern.compile("(.*\\S)\\s*\\+\\s*(\\S.*)")),
            Map.entry("-",   Pattern.compile("(.*\\S)\\s*-\\s*(\\S.*)")),
            Map.entry("*",   Pattern.compile("(.*[^\\s*])\\s*\\*\\s*([^\\s*].*)")),
            Map.entry("/",   Pattern.compile("(.*\\S)\\s*/\\s*(\\S.*)")),
            Map.entry("%",   Pattern.compile("(.*\\S)\\s*%\\s*(\\S.*)")),
            Map.entry("**",  Pattern.compile("(.*\\S)\\s*\\*\\*\\s*(\\S.*)")),
            Map.entry("==",  Pattern.compile("(.*\\S)\\s*==\\s*(\\S.*)")),
            Map.entry("!=",  Pattern.compile("(.*\\S)\\s*!=\\s*(\\S.*)")),
            Map.entry(">",   Pattern.compile("(.*\\S)\\s*>\\s*([^\\s=].*)")),
            Map.entry("<",   Pattern.compile("(.*\\S)\\s*<\\s*([^\\s=].*)")),
            Map.entry(">=",  Pattern.compile("(.*\\S)\\s*>=\\s*(\\S.*)")),
            Map.entry("<=",  Pattern.compile("(.*\\S)\\s*<=\\s*(\\S.*)")),
            Map.entry("&&",  Pattern.compile("(.*\\S)\\s*&&\\s*(\\S.*)")),
            Map.entry("||",  Pattern.compile("(.*\\S)\\s*\\|\\|\\s*(\\S.*)")),
            Map.entry("!",   Pattern.compile("!\\s*([^\\s=].*)"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (oldValue, newValue) -> oldValue,
            LinkedHashMap::new
    ));
}
