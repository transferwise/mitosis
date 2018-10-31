package com.transferwise.mitosis;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.transferwise.mitosis.ExperimentSerializer.deserialize;
import static com.transferwise.mitosis.ExperimentSerializer.serialize;

public class ExperimentFilter implements Filter {
    private final int cookieExpiry;
    private final String cookieName;
    private final String requestAttribute;
    private final String requestParameter;
    private final ExperimentEngine experimentEngine;
    private final String blacklistPath;

    private ExperimentFilter(Builder builder) {
        this.cookieExpiry = builder.cookieExpiry;
        this.cookieName = builder.cookieName;
        this.requestAttribute = builder.requestAttribute;
        this.requestParameter = builder.requestParameter;
        this.blacklistPath = builder.blacklistPath;
        experimentEngine = new ExperimentEngine();
    }

    public static class Builder {
        /**
         * Expiration time of cookie in seconds
         */
        private int cookieExpiry = 3600 * 24 * 30;//a month

        /**
         * Name of the cookie to keep the experimetns
         */
        private String cookieName = "ab";

        /**
         * Name of the request attribute to pass down the experiments assigned
         */
        private String requestAttribute = "experiments";

        /**
         * Name of parameter of the request for manual experiment activation
         */
        private String requestParameter = "activate";

        /**
         * Path for blacklisting running experiments
         */
        private String blacklistPath;

        public Builder cookieExpiry(int cookieExpiry) {
            this.cookieExpiry = cookieExpiry;
            return this;
        }

        public Builder cookieName(String cookieName) {
            this.cookieName = cookieName;
            return this;
        }

        public Builder requestAttribute(String requestAttribute) {
            this.requestAttribute = requestAttribute;
            return this;
        }

        public Builder requestParameter(String requestParameter) {
            this.requestParameter = requestParameter;
            return this;
        }

        public Builder blacklistPath(String blacklistPath) {
            this.blacklistPath = blacklistPath;
            return this;
        }

        public ExperimentFilter build() {
            return new ExperimentFilter(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExperimentFilter defaults() {
        return builder().build();
    }

    @Deprecated
    public ExperimentFilter prepare(String name, List<String> variants) {
        prepare(name, variants, null);

        return this;
    }

    @Deprecated
    public ExperimentFilter prepare(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        experimentEngine.register(new UserExperiment(name, variants, filter));

        return this;
    }

    public ExperimentFilter prepare(Experiment experiment) {
        experimentEngine.register(experiment);

        return this;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (blacklistPath != null && request.getServletPath().startsWith(blacklistPath)) {
            chain.doFilter(request, response);
            return;
        }

        Map<String, String> experiments = experimentEngine.refreshVariants(existingExperiments(request), request);

        String parameterExperiments = servletRequest.getParameter(requestParameter);
        if (parameterExperiments != null) {
            experiments.putAll(experimentEngine.cleanVariants(deserialize(parameterExperiments)));
        }

        request.setAttribute(requestAttribute, experiments);
        response.addCookie(createCookie(experiments));

        chain.doFilter(request, response);
    }

    private Map<String, String> existingExperiments(HttpServletRequest r) {
        Map<String, String> all = new HashMap<>();

        Cookie cookie = getCookie(r, cookieName);
        if (cookie != null) {
            all.putAll(deserialize(urlDecode(cookie.getValue())));
        }

        return all;
    }

    private static Cookie getCookie(HttpServletRequest r, String name) {
        return Arrays
                .stream(r.getCookies() != null ? r.getCookies() : new Cookie[0])
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
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

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
