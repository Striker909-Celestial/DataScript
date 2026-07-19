package com.striker.datascript;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.striker.datascript.objects.ScriptStructure;

import java.io.File;
import java.util.Map;

public class Compiler {

    public static void main(String[] args) {
        try {
            TomlMapper mapper = new TomlMapper();

            Map<String, Object> data = mapper.readValue(new File("src/main/java/com/striker/datascript/tests/data_test.toml"), Map.class);
            System.out.println(data);
            ScriptStructure structure = new ScriptStructure("data_test", data, s -> () -> ScriptStructure.EMPTY);
            System.out.println(structure.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
