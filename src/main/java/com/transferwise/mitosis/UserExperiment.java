package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class UserExperiment extends Experiment
{
    public UserExperiment(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        super(name, variants, filter);
    }

    public UserExperiment(String name, List<String> variants) {
        super(name, variants, null);
    }

    @Override
    public String chooseVariant(HttpServletRequest request) {
        int index = ThreadLocalRandom.current().nextInt(variants.size());
        Iterator<String> i = variants.iterator();
        for (int j = 0; j < index; j++) {
            i.next();
        }
        return i.next();
    }
}
