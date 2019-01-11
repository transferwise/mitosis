package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExperimentEngine {

    private final Set<Experiment> experiments = new HashSet<>();

    public void register(Experiment experiment) {
        experiments.add(experiment);
    }

    public Map<String, String> refreshVariants(Map<String, String> variants, HttpServletRequest request) {
        return experiments.stream()
                .filter(e -> e.filter == null || e.filter.test(request))
                .collect(Collectors.toMap(e -> e.name, e -> refresh(e, request, variants.get(e.name))));
    }

    private String refresh(Experiment experiment, HttpServletRequest request, String variant) {
        if (variant == null) {
            return experiment.chooseVariant(request);
        }

        if (experiment instanceof SeoExperiment) {
            return experiment.chooseVariant(request);
        }

        if (!experiment.isValidVariant(variant)) {
            return experiment.chooseVariant(request);
        }

        return variant;
    }

    public Map<String, String> cleanVariants(Map<String, String> variants) {
        return experiments.stream()
                .filter(e -> variants.containsKey(e.name) && e.isValidVariant(variants.get(e.name)))
                .collect(Collectors.toMap(e -> e.name, e -> variants.get(e.name)));
    }
}
