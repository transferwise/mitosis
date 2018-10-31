package com.transferwise.mitosis;

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import java.util.function.Predicate;

class ExperimentEngineSpec extends Specification {

    private ExperimentEngine engine

    def setup() {
        engine = new ExperimentEngine()
    }

    def 'it generates variants for registered experiments'() {
        setup:
        Map<String, String> variants = new HashMap<>()
        engine.register(new SeoExperiment('test1', ['a']))
        engine.register(new UserExperiment('test2', ['b']))

        when:
        variants = engine.refreshVariants(variants, createRequestMock('path'))

        then:
        2 == variants.size()
        variants.get('test1') == 'a'
        variants.get('test2') == 'b'
    }

    def 'it applies experiment filters'() {
        setup:
        Map<String, String> variants = new HashMap<>()
        engine.register(new UserExperiment('test1', ['a']))
        engine.register(new UserExperiment('test2', ['b'], new Predicate<HttpServletRequest>() {
            @Override
            boolean test(HttpServletRequest request) {
                return false
            }
        }))

        when:
        variants = engine.refreshVariants(variants, createRequestMock('path'))

        then:
        1 == variants.size()
    }

    def 'it does not override valid variants'() {
        setup:
        Map<String, String> variants = new HashMap<String, String>() {{ put('test', 'a') }}
        engine.register(new UserExperiment('test', ['a', 'b', 'c', 'd']))

        when: variants = engine.refreshVariants(variants, createRequestMock('path'))

        then: variants.get('test', 'a')
    }

    def 'it overrides invalid variants'() {
        setup:
        Map<String, String> variants = new HashMap<String, String>() {{ put('test', 'b') }}
        engine.register(new UserExperiment('test', ['a']))

        when: variants = engine.refreshVariants(variants, createRequestMock('path'))

        then: variants.get('test', 'a')
    }

    def 'it cleans invalid experiments'() {
        setup:
        Map<String, String> variants = new HashMap<String, String>() {{ put('test', 'a') }}

        when: variants = engine.refreshVariants(variants, createRequestMock('path'))

        then: variants.size() == 0
    }

    protected HttpServletRequest createRequestMock(path) {
        def request = Mock(HttpServletRequest)
        request.getServletPath() >> { path }

        request
    }
}
