package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Predicate;

public abstract class Experiment {

    private static final String NAME_FORMAT = "^[a-z](-?[a-z0-9])*$";

    protected String name;
    protected List<String> variants;
    protected Predicate<HttpServletRequest> filter;

    public Experiment(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        assertValidName(name);
        assertValidVariants(variants);

        this.name = name;
        this.variants = variants;
        this.filter = filter;
    }

    public abstract String chooseVariant(HttpServletRequest request);

    static void assertValidName(String name) {
        if (!name.matches(NAME_FORMAT)) {
            throw new RuntimeException("Invalid experiment name value " + name);
        }
    }

    static void assertValidVariants(List<String> variants) {
        if (variants == null) {
            throw new RuntimeException("A list of variants must be provided");
        }

        if (variants.size() < 1) {
            throw new RuntimeException("Experiment must contain at least one variant");
        }

        variants.forEach(Experiment::assertValidName);
    }

    public boolean isValidVariant(String variant) {
        return variants.contains(variant);
    }
}
