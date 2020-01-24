package com.transferwise.mitosis

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.FilterChain
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CookieConsentSpec extends Specification {

    private static final String EXPERIMENT_COOKIE_NAME = '_ab'
    private static final String CONSENT_COOKIE_NAME = 'consentCookie'
    private static final String REQUEST_ATTRIBUTE = 'experiments'
    private static final String REQUEST_PARAMETER = 'experiments'

    private ExperimentFilter filter
    private HttpServletRequest request
    private HttpServletResponse response
    private Cookie consentCookie
    private Cookie experimentCookie
    private Map<String, String> experiments  // will be added to the request attributes
    private String path = '/default'
    private String parameter = ''

    def 'setup'() {
        request = createRequestMock()
        response = createResponseMock()
    }

    @Unroll
    def 'it should throw if cookieConsentAcceptedRegex #cookieConsentAcceptedRegex is invalid'() {
        when:
        filter = ExperimentFilter.builder()
                .cookieConsent(CONSENT_COOKIE_NAME, cookieConsentAcceptedRegex)
                .build()

        then:
        thrown RuntimeException

        where:
        cookieConsentAcceptedRegex << [null, '[']
    }

    def 'it should set cookies if consent not required'() {
        setup:
        filter = ExperimentFilter.builder()
                .cookieName(EXPERIMENT_COOKIE_NAME)
                .requestAttribute(REQUEST_ATTRIBUTE)
                .requestParameter(REQUEST_PARAMETER)
                .build()
        filter.prepare(new UserExperiment('test', ['a']))

        consentCookie = null

        // http://spockframework.org/spock/docs/1.0/interaction_based_testing.html#_combining_mocking_and_stubbing
        1 * response.addCookie(_) >> { Cookie c -> experimentCookie = c }


        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        experimentCookieContains('test', 'a')
        experiments.test == 'a'
    }

    def 'it should not set cookies if cookie consent required but consent cookie does not exist'() {
        setup:
        filter = ExperimentFilter.builder()
                .cookieName(EXPERIMENT_COOKIE_NAME)
                .requestAttribute(REQUEST_ATTRIBUTE)
                .requestParameter(REQUEST_PARAMETER)
                .cookieConsent(CONSENT_COOKIE_NAME, "\"status\":\"accepted\"")
                .build()
        filter.prepare(new UserExperiment('test', ['a']))

        consentCookie = null
        experimentCookie = null

        0 * response.addCookie(_) >> { Cookie c -> experimentCookie = c }


        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        experimentCookie == null
        experiments.isEmpty()
    }

    @Unroll
    def 'it should set request attributes for SEO experiments regardless of cookie consent'() {
        setup:
        filter = ExperimentFilter.builder()
                .cookieName(EXPERIMENT_COOKIE_NAME)
                .requestAttribute(REQUEST_ATTRIBUTE)
                .requestParameter(REQUEST_PARAMETER)
                .cookieConsent(CONSENT_COOKIE_NAME, '^1$')
                .build()
        filter.prepare(new UserExperiment('user', ['a']))
        filter.prepare(new SeoExperiment('seo', ['a']))

        consentCookie = new Cookie(CONSENT_COOKIE_NAME, URLEncoder.encode(cookieConsentCookieValue, "UTF-8"))
        experimentCookie = null

        response.addCookie(_) >> { Cookie c -> experimentCookie = c }

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        experiments.seo == 'a'

        where:
        cookieConsentCookieValue << ['1', '0']
    }

    @Unroll
    def 'it should not set cookies if cookie consent required and cookie consent rejected'() {
        setup:
        filter = ExperimentFilter.builder()
                .cookieName(EXPERIMENT_COOKIE_NAME)
                .requestAttribute(REQUEST_ATTRIBUTE)
                .requestParameter(REQUEST_PARAMETER)
                .cookieConsent(CONSENT_COOKIE_NAME, cookieConsentAcceptedRegex)
                .build()
        filter.prepare(new UserExperiment('test', ['a']))

        consentCookie = new Cookie(CONSENT_COOKIE_NAME, URLEncoder.encode(cookieConsentCookieValue, "UTF-8"))

        0 * response.addCookie(_) >> { Cookie c -> experimentCookie = c }

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        experimentCookie == null
        experiments.isEmpty()

        where:
        cookieConsentAcceptedRegex | cookieConsentCookieValue
        '\"status\":\"accepted\"'  | '{\"status\":\"rejected\"}'
        '^true$'                   | 'false'
        '^1$'                      | '0'
    }

    @Unroll
    def 'it should set cookies if cookie consent required and cookie consent accepted'() {
        setup:
        filter = ExperimentFilter.builder()
                .cookieName(EXPERIMENT_COOKIE_NAME)
                .requestAttribute(REQUEST_ATTRIBUTE)
                .requestParameter(REQUEST_PARAMETER)
                .cookieConsent(CONSENT_COOKIE_NAME, cookieConsentAcceptedRegex)
                .build()
        filter.prepare(new UserExperiment('test', ['a']))

        consentCookie = new Cookie(CONSENT_COOKIE_NAME, URLEncoder.encode(cookieConsentCookieValue, "UTF-8"))

        1 * response.addCookie(_) >> { Cookie c -> experimentCookie = c }

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        experimentCookieContains("test", "a")
        experiments.test == 'a'

        where:
        cookieConsentAcceptedRegex    | cookieConsentCookieValue
        '\"status\":\"accepted\"'     | '{\"status\":\"accepted\"}'
        '.*\"status\":\"accepted\".*' | '{\"status\":\"accepted\"}'
        '^true$'                      | 'true'
        '^1$'                         | '1'
    }

    private boolean experimentCookieContains(String test, String experiment) {
        URLDecoder.decode(experimentCookie.value, 'utf-8').contains(test + ExperimentSerializer.VARIANT_SEPARATOR + experiment)
    }

    private HttpServletRequest createRequestMock() {
        def request = Mock(HttpServletRequest)
        request.getCookies() >> {
            if (consentCookie == null && experimentCookie == null) {
                return []
            }

            if (consentCookie == null && experimentCookie != null) {
                return [experimentCookie]
            }

            if (consentCookie != null && experimentCookie == null) {
                return [consentCookie]
            }

            return [consentCookie, experimentCookie]
        }
        request.setAttribute(REQUEST_ATTRIBUTE, _) >> { _, Map<String, String> exps -> experiments = exps }
        request.getParameter(REQUEST_PARAMETER) >> { parameter }
        request.getServletPath() >> { path }

        request
    }

    private HttpServletResponse createResponseMock() {
        def response = Mock(HttpServletResponse)

        response
    }
}
