package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ExperimentEngine {
    private static final String NAME_FORMAT = "^[a-z](-?[a-z0-9])*$";

    private final Map<String, Set<String>> experimentConfig = new HashMap<>();
    private final Map<String, Predicate<HttpServletRequest>> filters = new HashMap<>();

    void prepare(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        assertValidName(name);
        variants.forEach(ExperimentEngine::assertValidName);

        experimentConfig.put(name, new HashSet<>(variants));

        if (filter != null) {
            filters.put(name, filter);
        }
    }

    private static void assertValidName(String name) {
        if (!name.matches(NAME_FORMAT)) {
            throw new RuntimeException("Invalid experiment name value " + name);
        }
    }

    Map<String, String> calculateExperiments(Map<String, String> existingExperiments, HttpServletRequest request) {
        Map<String, String> cleanedExperiments = new HashMap<>(clean(existingExperiments));

        Map<String, String> newExperiments = differenceWith(cleanedExperiments.keySet())
                .stream()
                .filter(experiment -> {
                    Predicate<HttpServletRequest> filter = filters.get(experiment);
                    return filter == null || filter.test(request);
                })
                .collect(Collectors.toMap(Function.identity(), this::pickVariantFor));


        cleanedExperiments.putAll(newExperiments);

        return cleanedExperiments;
    }

    private Map<String, String> clean(Map<String, String> requestExperiments) {
        return removeInvalidVariants(removeInvalidExperiments(requestExperiments));
    }

    private Map<String, String> removeInvalidExperiments(Map<String, String> experiments) {
        Map<String, String> cleaned = new HashMap<>(experiments);
        cleaned.keySet().retainAll(experimentConfig.keySet());

        return cleaned;
    }

    private Map<String, String> removeInvalidVariants(Map<String, String> experiments) {
        return experiments.entrySet()
                .stream()
                .filter(e -> isValidVariant(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String pickVariantFor(String experiment) {
        Set<String> variations = experimentConfig.get(experiment);
        int index = ThreadLocalRandom.current().nextInt(variations.size());
        Iterator<String> i = variations.iterator();
        for (int j = 0; j < index; j++) {
            i.next();
        }
        return i.next();
    }

    private Set<String> differenceWith(Set<String> currentExperiments) {
        Set<String> difference = new HashSet<>(experimentConfig.keySet());
        difference.removeAll(currentExperiments);
        return difference;
    }

    private boolean isValidVariant(String experiment, String variant) {
        return experimentConfig.get(experiment).contains(variant);
    }
}
