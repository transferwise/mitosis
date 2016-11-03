package com.transferwise.mitosis;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExperimentFilter implements Filter {
    static final String VARIANT_SEPARATOR = ":";
    static final String EXPERIMENT_SEPARATOR = ",";
    private static final String FORMAT = "^[a-z](-?[a-z0-9])*$";

    private final int cookieExpiry;
    private final String cookieName;
    private final String requestAttribute;
    private final String requestParameter;
    private Map<String, Set<String>> experiments = new HashMap<>();
    private Map<String, Predicate<HttpServletRequest>> filters = new HashMap<>();

    public ExperimentFilter(int cookieExpiry, String cookieName, String requestAttribute, String requestParameter) {
        this.cookieExpiry = cookieExpiry;
        this.cookieName = cookieName;
        this.requestAttribute = requestAttribute;
        this.requestParameter = requestParameter;
    }

    public void prepare(String name, List<String> variants) {
        prepare(name, variants, null);
    }

    public void prepare(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        assertValidValue(name);
        variants.forEach(ExperimentFilter::assertValidValue);

        experiments.put(name, new HashSet<>(variants));

        if (filter != null) {
            filters.put(name, filter);
        }
    }

    private static void assertValidValue(String value) {
        if (!value.matches(FORMAT)) {
            throw new RuntimeException("Invalid value " + value);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        Map<String, String> currentExperiments = clean(currentExperiments(request));
        currentExperiments.putAll(experimentsMissingIn(currentExperiments, request));

        request.setAttribute(requestAttribute, currentExperiments);
        response.addCookie(createCookie(currentExperiments));

        chain.doFilter(request, response);
    }

    private Map<String, String> experimentsMissingIn(Map<String, String> currentExperiments, HttpServletRequest request) {
        return differenceWith(currentExperiments.keySet())
                .stream()
                .filter(experiment -> {
                    Predicate<HttpServletRequest> filter = filters.get(experiment);
                    return filter == null || filter.test(request);
                })
                .collect(Collectors.toMap(Function.identity(), this::pickVariantFor));
    }

    private String pickVariantFor(String experiment) {
        Set<String> variations = experiments.get(experiment);
        int index = ThreadLocalRandom.current().nextInt(variations.size());
        Iterator<String> i = variations.iterator();
        for (int j = 0; j < index; j++) {
            i.next();
        }
        return i.next();
    }

    private Set<String> differenceWith(Set<String> currentExperiments) {
        Set<String> difference = new HashSet<>(experiments.keySet());
        difference.removeAll(currentExperiments);
        return difference;
    }

    private Map<String, String> clean(Map<String, String> currentExperiments) {
        return cleanVariants(cleanExperiments(currentExperiments));
    }

    private Map<String, String> cleanExperiments(Map<String, String> currentExperiments) {
        Map<String, String> cleaned = new HashMap<>(currentExperiments);
        cleaned.keySet().retainAll(experiments.keySet());

        return cleaned;
    }

    private Map<String, String> cleanVariants(Map<String, String> experiments) {
        return experiments.entrySet()
                .stream()
                .filter(e -> isValidVariant(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isValidVariant(String experiment, String variant) {
        return experiments.get(experiment).contains(variant);
    }

    private Map<String, String> currentExperiments(HttpServletRequest r) {
        Map<String, String> all = new HashMap<>();
        all.putAll(cookieExperiments(r));
        all.putAll(parameterExperiments(r));

        return all;
    }

    private Map<String, String> parameterExperiments(HttpServletRequest r) {
        String parameterExperiments = r.getParameter(requestParameter);

        if (parameterExperiments == null) {
            return new HashMap<>();
        }

        return deserialize(parameterExperiments);
    }

    private Map<String, String> cookieExperiments(HttpServletRequest r) {
        return Arrays
                .stream(getCookies(r))
                .filter(c -> c.getName().equals(cookieName))
                .findFirst()
                .map(c -> deserialize(urlDecode(c.getValue())))
                .orElse(new HashMap<>());
    }

    private static Map<String, String> deserialize(String value) {
        return Arrays
                .stream(value.split(Pattern.quote(EXPERIMENT_SEPARATOR)))
                .filter(exp -> exp.contains(VARIANT_SEPARATOR))
                .collect(Collectors.toMap(
                        exp -> exp.split(VARIANT_SEPARATOR)[0],
                        exp -> exp.split(VARIANT_SEPARATOR)[1]
                ));
    }

    private static Cookie[] getCookies(HttpServletRequest r) {
        return r.getCookies() != null ? r.getCookies() : new Cookie[0];
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Cookie createCookie(Map<String, String> experiments) {
        Cookie c = new Cookie(cookieName, urlEncode(serialize(experiments)));
        c.setMaxAge(cookieExpiry);
        c.setPath("/");
        return c;
    }

    private static String serialize(Map<String, String> experiments) {
        return experiments.entrySet()
                .stream()
                .map(e -> e.getKey() + VARIANT_SEPARATOR + e.getValue())
                .collect(Collectors.joining(EXPERIMENT_SEPARATOR));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
