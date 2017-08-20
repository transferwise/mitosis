package com.transferwise.mitosis;

import javax.servlet.*;
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

    public ExperimentFilter(int cookieExpiry, String cookieName, String requestAttribute, String requestParameter) {
        this.cookieExpiry = cookieExpiry;
        this.cookieName = cookieName;
        this.requestAttribute = requestAttribute;
        this.requestParameter = requestParameter;
        experimentEngine = new ExperimentEngine();
    }

    public static ExperimentFilter defaults() {
        return new ExperimentFilter(3600 * 24 * 30, "ab", "experiments", "activate");
    }

    public void prepare(String name, List<String> variants) {
        prepare(name, variants, null);
    }

    public void prepare(String name, List<String> variants, Predicate<HttpServletRequest> filter) {
        experimentEngine.prepare(name, variants, filter);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        Map<String, String> experiments = experimentEngine.calculateExperiments(existingExperiments(request), request);

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

        String parameterExperiments = r.getParameter(requestParameter);
        if (parameterExperiments != null) {
            all.putAll(deserialize(parameterExperiments));
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
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
