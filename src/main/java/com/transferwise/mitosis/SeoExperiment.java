package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Predicate;

public class SeoExperiment extends Experiment {

    public SeoExperiment(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        super(name, variants, filter);
    }

    public SeoExperiment(String name, List<String> variants) {
        super(name, variants, null);
    }

    @Override
    public String chooseVariant(HttpServletRequest request) {
        int index = Math.abs(request.getServletPath().hashCode()) % variants.size();
        return variants.get(index);
    }
}
