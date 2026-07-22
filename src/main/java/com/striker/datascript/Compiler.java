package com.striker.datascript;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.striker.datascript.objects.ScriptFunction;
import com.striker.datascript.objects.ScriptObject;
import com.striker.datascript.objects.ScriptStructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Compiler {

    private static final TomlMapper TOML_MAPPER = new TomlMapper();

    private final Map<String, ScriptStructure> files;

    private Compiler(Map<String, Map<String, Object>> files) {
        this.files = new HashMap<>();
        for (String filename : files.keySet()) {
            Map<String, Object> data = files.get(filename);
            ScriptStructure structure = new ScriptStructure(filename, data, this::importContext);
            this.files.put(filename, structure);
        }
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Map<String, Map<String, Object>> files;

        public Builder() {
            files = new HashMap<>();
            files.put("java", new HashMap<>());
        }

        public Builder addFile(File file) throws IOException {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = TOML_MAPPER.readValue(file, Map.class);
            files.put(file.getName(), data);
            return this;
        }

        public Builder addFile(String path) throws IOException {
            return this.addFile(Path.of(path).toAbsolutePath().toFile());
        }

        public Builder addJavaFunction(String name, ScriptFunction<?> function) {
            files.get("java").put(name, function);
            return this;
        }

        public Compiler build() {
            return new Compiler(files);
        }
    }

    public Supplier<ScriptObject<?>> importContext(String reference) {
        String[] split = reference.substring(1).split("\\.");
        if (!split[0].equals("import") || split.length < 3) { return null; }
        String filename = split[1];
        StringBuilder localRef = new StringBuilder(String.valueOf(reference.charAt(0)));
        for (int i = 2; i < split.length; i++) {
            localRef.append(split[i]).append(".");
        }
        localRef.deleteCharAt(localRef.length() - 1);
        return () -> {
            if (files.containsKey(filename)) {
                return files.get(filename).get(localRef.toString());
            }
            throw new RuntimeException("File not found or not compiled: " + filename);
        };
    }

    public ScriptObject<?> get(String path) {
        char prefix = path.charAt(0);
        String[] split = path.split("\\.");
        if (split.length == 1) {
            if (prefix == '$' || prefix == '@') { path = path.substring(1); }
            return files.get(path);
        } else {
            StringBuilder localRef = new StringBuilder();
            if (prefix != '$' && prefix != '@') { localRef.append("$"); }
            for (int i = 1; i < split.length; i++) {
                localRef.append(".").append(split[i]);
            }
            return files.get(split[0]).get(localRef.toString());
        }
    }

    public ScriptObject<?> run(String path, Map<String, ScriptObject<?>> args) {
        ScriptObject<?> obj = this.get(path);
        if (obj instanceof ScriptFunction<?> func) {
            return func.apply(new ScriptStructure(args));
        }
        return obj;
    }

    public ScriptObject<?> run(String path) {
        return this.run(path, new HashMap<>());
    }

    public String toString() { return this.files.toString(); }
}
