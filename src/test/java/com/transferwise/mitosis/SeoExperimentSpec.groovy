package com.transferwise.mitosis;

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest;

class SeoExperimentSpec extends Specification {

    @Unroll
    def 'experiment name #name should be invalid'() {
        when: new SeoExperiment(name, [])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    @Unroll
    def 'variant name #name should be invalid'() {
        when: new SeoExperiment('valid', [name])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    def 'null variants should be invalid'() {
        when: new SeoExperiment('test', null)

        then: thrown RuntimeException
    }

    def 'empty variants should be invalid'() {
        when: new SeoExperiment('test', [])

        then: thrown RuntimeException
    }

    def 'it chooses a variant'() {
        given: Experiment experiment = new SeoExperiment('test', ['a'])

        when: String variant = experiment.chooseVariant(createRequestMock('path'))

        then: "a" == variant
    }

    def 'variant is deterministic'() {
        given: Experiment experiment = new SeoExperiment('test', ['a', 'b', 'c'])

        when: String variant = experiment.chooseVariant(createRequestMock('path'))

        then: 'a' == variant
    }

    def 'variant depends on path'() {
        given: Experiment experiment = new SeoExperiment('test', ['a', 'b', 'c'])

        when:
            String variant1 = experiment.chooseVariant(createRequestMock('path1'))
            String variant2 = experiment.chooseVariant(createRequestMock('path2'))

        then:
            'b' == variant1
            'c' == variant2
    }

    protected HttpServletRequest createRequestMock(path) {
        def request = Mock(HttpServletRequest)
        request.getServletPath() >> { path }

        request
    }
}
