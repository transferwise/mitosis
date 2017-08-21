package com.transferwise.mitosis

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.FilterChain
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.function.Predicate

class ExperimentFilterSpec extends Specification {
    private static final String COOKIE_NAME = '_ab'
    private static final String REQUEST_ATTRIBUTE = 'experiments'
    private static final String REQUEST_PARAMETER = 'experiments'

    ExperimentFilter filter
    HttpServletRequest request
    HttpServletResponse response
    Cookie cookie = new Cookie(COOKIE_NAME, 'irrelevant')
    Map<String, String> experiments = [:]
    String parameter = ''

    def setup() {
        filter = new ExperimentFilter(3600, COOKIE_NAME, REQUEST_ATTRIBUTE, REQUEST_PARAMETER);
        request = createRequestMock()
        response = createResponseMock()
    }

    def 'it creates an experiment filter'() {
        expect: ExperimentFilter.defaults()
    }

    @Unroll
    def 'experiment name #name should be invalid'() {
        when: filter.prepare(name, [])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    @Unroll
    def 'variant name #name should be invalid'() {
        when: filter.prepare('valid', [name])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    def 'it does not override previous experiments'() {
        setup:
            setupCookie([test: 'b'])
            filter.prepare('test', ['a', 'b'])

        when: doFilter()

        then:
            cookieContains('test', 'b')
            experiments.test == 'b'
    }

    def 'it chooses a variant'() {
        setup: filter.prepare('test', ['a'])

        when: doFilter()

        then:
            cookieContains('test', 'a')
            experiments.test == 'a'
    }

    def 'it initialises multiple experiments'() {
        setup:
            filter.prepare('test1', ['a'])
            filter.prepare('test2', ['b'])

        when: doFilter()

        then:
            cookieContains('test1', 'a')
            cookieContains('test2', 'b')
            experiments.test1 == 'a'
            experiments.test2 == 'b'
    }

    def 'it cleans malformed experiments'() {
        setup: cookie.value = 'irrelevant'

        when: doFilter()

        then: cookie.value == ''
    }

    def 'it cleans invalid experiments'() {
        setup:
            cookie.value = 'test1:a'
            filter.prepare('test2', ['b'])

        when: doFilter()

        then: cookieContains('test2', 'b')
    }

    def 'it resets invalid variants'() {
        setup:
            cookie.value = 'test:b'
            filter.prepare('test', ['a'])

        when: doFilter()

        then:
            cookieContains('test', 'a')
            experiments.test == 'a'
    }

    def 'it overrides cookie when parameter provided'() {
        setup:
            parameter = 'test:a'
            cookie.value = 'test:b'
            filter.prepare('test', ['a'])

        when: doFilter()

        then:
            cookieContains('test', 'a')
            experiments.test == 'a'
    }

    def 'it should pass filter and run the experiment'() {
        setup:
            setupRequestLocaleCountryToBe('GB')

        filter.prepare('test', ['a'], requestLocaleCountryFilter('GB'))

        when: doFilter()

        then:
            cookieContains('test', 'a')
            experiments.test == 'a'
    }

    def 'it should not run the experiment based on filter'() {
        setup:
            setupRequestLocaleCountryToBe('US')
            filter.prepare('test', ['a'], requestLocaleCountryFilter('GB'))

        when: doFilter()

        then: cookie.value == ''
    }

    private setupCookie(Map map) {
        cookie.value = URLEncoder.encode(map
                .collect { it.key + ExperimentSerializer.VARIANT_SEPARATOR + it.value}
                .join(ExperimentSerializer.EXPERIMENT_SEPARATOR), 'utf-8')
    }

    private boolean cookieContains(String test, String experiment) {
        URLDecoder.decode(cookie.value, 'utf-8').contains(test + ExperimentSerializer.VARIANT_SEPARATOR + experiment)
    }

    private setupRequestLocaleCountryToBe(String country) {
        request.getLocale() >> new Locale('en', country)
    }

    private static Predicate<HttpServletRequest> requestLocaleCountryFilter(String country) {
        return { it.getLocale().country == country }
    }

    private doFilter() {
        filter.doFilter(request, response, Mock(FilterChain))
    }

    private HttpServletRequest createRequestMock() {
        def request = Mock(HttpServletRequest)
        request.getCookies() >> { cookie }
        request.setAttribute(REQUEST_ATTRIBUTE, _) >> { _, Map<String, String> exps -> experiments = exps }
        request.getParameter(REQUEST_PARAMETER) >> { parameter }

        request
    }

    private HttpServletResponse createResponseMock() {
        def response = Mock(HttpServletResponse)
        response.addCookie(_) >> { Cookie c -> cookie = c }

        response
    }
}
