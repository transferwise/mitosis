package com.transferwise.mitosis;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.function.Predicate;

public class RequestFilter {
    public static Predicate<HttpServletRequest> pathEquals(String path) {
        return request -> request.getRequestURI().equals(path);
    }

    public static Predicate<HttpServletRequest> pathContains(String path) {
        return request -> request.getRequestURI().contains(path);
    }

    public static Predicate<HttpServletRequest> languageEquals(String language) {
        return request -> request.getLocale().getLanguage().equals(new Locale(language).getLanguage());
    }

    public static Predicate<HttpServletRequest> userAgentContains(String userAgent) {
        return headerContains("user-agent", userAgent);
    }

    public static Predicate<HttpServletRequest> headerContains(String header, String value) {
        return request -> {
            String headerValue = request.getHeader(header);
            return headerValue != null && headerValue.toLowerCase().contains(value.toLowerCase());
        };
    }
}
