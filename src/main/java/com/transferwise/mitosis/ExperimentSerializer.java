package com.transferwise.mitosis;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ExperimentSerializer {
    static final String VARIANT_SEPARATOR = ":";
    static final String EXPERIMENT_SEPARATOR = ",";

    static String serialize(Map<String, String> experiments) {
        return experiments.entrySet()
                .stream()
                .map(e -> e.getKey() + VARIANT_SEPARATOR + e.getValue())
                .collect(Collectors.joining(EXPERIMENT_SEPARATOR));
    }

    static Map<String, String> deserialize(String value) {
        return Arrays
                .stream(value.split(Pattern.quote(EXPERIMENT_SEPARATOR)))
                .filter(exp -> exp.contains(VARIANT_SEPARATOR))
                .collect(Collectors.toMap(
                        exp -> exp.split(VARIANT_SEPARATOR)[0],
                        exp -> exp.split(VARIANT_SEPARATOR)[1]
                ));
    }
}
