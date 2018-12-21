package com.transferwise.mitosis

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static com.transferwise.mitosis.RequestFilter.*

class RequestFilterSpec extends Specification {
    @Unroll
    def 'it tests request filters'() {
        expect:
            result == filter.test(aRequest('/a/path', Locale.UK, 'Googlebot'))

        where:
            filter                                    | result
            pathEquals('/a/path')                     | true
            pathEquals('/PATH')                       | false
            pathContains('/path')                     | true
            pathContains('//')                        | false
            languageEquals('en')                      | true
            languageEquals('es')                      | false
            userAgentContains('GoogleBot')            | true
            userAgentContains('FacebookBot')          | false
            headerContains('user-agent', 'GoogleBot') | true
            headerContains('special', 'irrelevant')   | false
    }

    def 'it applies filter to percentage of paths'() {
        expect:
        int count = 0
        for (int i = 0; i < reps; i++) {
            if (applyToPercentageOfPaths(30).test(aRequest('path' + i, Locale.UK, 'GoogleBot'))) {
                count++
            }
        }
        count == result

        where:
        reps | result
        10   | 4
        100  | 32
        1000 | 305
    }

    private aRequest(path, locale, userAgent) {
        def r = Mock(HttpServletRequest)
        r.getRequestURI() >> path
        r.getServletPath() >> path
        r.getLocale() >> locale
        r.getHeader('user-agent') >> userAgent
        r
    }
}
