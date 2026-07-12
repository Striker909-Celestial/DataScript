package com.striker.datascript.objects;
import com.striker.datascript.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScriptString implements ScriptObject<Object> {

    public static final ScriptString EMPTY = new ScriptString("");

    private static int idNum = 0;

    private enum Type {
        PLAINTEXT,
        FTEXT,
        REFERENCE,
        FUNCTION,
        ERROR
    }

    private final Pattern INSERT_PATTERN = Pattern.compile("(?:^|[^\\\\])\\{(.*[^\\\\])}");
    private final Pattern PARENTHETICAL_PATTERN = Pattern.compile("(?:^|[^\\\\])\\((.*[^\\\\])\\)");
    private final Pattern REFERENCE_PATTERN = Pattern.compile("(?:^|[^\\\\])(\\$\\S+)");
    private final Pattern MUT_PATTERN = Pattern.compile("(?:^|[^\\\\])(@\\S+)");

    private final Matcher insertMatcher;
    private final Matcher parentheticalMatcher;
    private final Matcher referenceMatcher;
    private final Matcher mutMatcher;

    private final int id;
    public final String str;
    private final Type type;
    private final Function<String, Supplier<ScriptObject<?>>> context;
    private final Supplier<?> supplier;

    public ScriptString(String str, Function<String, Supplier<ScriptObject<?>>> context) {
        this.id = idNum++;
        this.str = str;
        this.insertMatcher = INSERT_PATTERN.matcher(str);
        this.parentheticalMatcher = PARENTHETICAL_PATTERN.matcher(str);
        this.referenceMatcher = REFERENCE_PATTERN.matcher(str);
        this.mutMatcher = MUT_PATTERN.matcher(str);
        this.type = detectType();

        this.context = context;
        this.supplier = switch (type) {
            case REFERENCE -> () -> this.context.apply(this.insertMatcher.group(1)).get().get();
            case PLAINTEXT -> () -> this.str;
            case FTEXT -> buildFTextSupplier();
            case FUNCTION -> buildFunctionSupplier();
            default -> () -> null;
        };
    }

    public ScriptString(String str) {
        this.id = idNum++;
        this.str = str;
        this.insertMatcher = INSERT_PATTERN.matcher(str);
        this.parentheticalMatcher = PARENTHETICAL_PATTERN.matcher(str);
        this.referenceMatcher = REFERENCE_PATTERN.matcher(str);
        this.mutMatcher = MUT_PATTERN.matcher(str);
        this.type = Type.PLAINTEXT;

        this.context = s -> null;
        this.supplier = () -> this.str;
    }

    private Type detectType() {
        if (insertMatcher.find()) { return Type.FTEXT; }
        if (!(referenceMatcher.find() || mutMatcher.find())) { return Type.PLAINTEXT; }
        if (referenceMatcher.results().count() + mutMatcher.results().count() == 1) { return Type.REFERENCE; }
        for (String operator : Core.OPERATORS.keySet()) {
            if (str.contains(operator)) { return Type.FUNCTION; }
        }
        return Type.ERROR;
    }

    private Supplier<?> buildFTextSupplier() {
        List<MatchResult> inserts = this.insertMatcher.results().toList();
        ArrayList<ScriptString> strings = new ArrayList<>();
        if (inserts.getFirst().start() != 0) { strings.add(new ScriptString(str.substring(0, inserts.getFirst().start()), context)); }
        for (MatchResult insert : inserts) {
            strings.add(new ScriptString(insert.group(1), context));
            if (insert.end() != str.length()) { strings.add(new ScriptString(str.substring(insert.end()), context)); }
        }
        return () -> {
            StringBuilder builder = new StringBuilder();
            for (ScriptString string : strings) { builder.append(string.get()); }
            return builder.toString();
        };
    }

    private Supplier<?> buildFunctionSupplier() {
        List<MatchResult> parentheticals = this.parentheticalMatcher.results().toList();
        String newStr = str;
        List<ScriptString> parentheticalResults = new ArrayList<>();
        for (int i = 0; i < parentheticals.size(); i++) {
            String parenthetical = parentheticals.get(i).group(1);
            newStr = newStr.replace(parenthetical, "$[" + this.id + "~" + i + "]");
            parentheticalResults.add(new ScriptString(parenthetical, context));
        }

        Pattern parentheicalRefPattern = Pattern.compile("\\$\\[" + this.id + "~(\\d)]");
        Function<String, Supplier<ScriptObject<?>>> newContext = s -> {
            if (parentheicalRefPattern.matcher(s).matches()) {
                return () -> parentheticalResults.get(Integer.parseInt(parentheicalRefPattern.matcher(s).group(1)));
            }
            return () -> context.apply(s).get();
        };

        for (String operator : Core.OPERATORS.keySet()) {
            Matcher matcher = Core.OPERATOR_PATTERNS.get(operator).matcher(newStr);
            if (matcher.find()) {
                int numGroups = matcher.groupCount();
                List<ScriptString> args = new ArrayList<>();
                for (int i = 1; i <= numGroups; i++) {
                    args.add(new ScriptString(matcher.group(i), newContext));
                }
                return () -> {
                    try {
                        return Core.OPERATORS.get(operator).apply(new ScriptArray(new ArrayList<>(args)));
                    } catch (Exception e) {
                        return null;
                    }
                };
            }
        }
        return null;
    }

    public Supplier<Object> supplier() { return supplier::get; }
    public Object get() { return supplier.get(); }
    public double comparisonNumber() {
        ScriptObject<?> output = ScriptObject.of(this.get());
        if (output == null) { return 0; }
        if (ScriptObject.matchType(output, EMPTY)) {
            String s = output.get().toString();
            double num = s.length();
            for (int i = (int) num; i > 0; i--) {
                num += 0.01 * i * (double) s.charAt(s.length() - i);
            }
            return num;
        }
        return output.comparisonNumber();
    }
}
