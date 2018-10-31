package com.transferwise.mitosis

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest;

class UserExperimentSpec extends Specification {

    @Unroll
    def 'experiment name #name should be invalid'() {
        when: new UserExperiment(name, [])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    @Unroll
    def 'variant name #name should be invalid'() {
        when: new UserExperiment('valid', [name])

        then: thrown RuntimeException

        where: name << ['INVALID', 'with spaces']
    }

    def 'null variants should be invalid'() {
        when: new UserExperiment('test', null)

        then: thrown RuntimeException
    }

    def 'empty variants should be invalid'() {
        when: new UserExperiment('test', [])

        then: thrown RuntimeException
    }

    def 'it chooses a variant'() {
        given: Experiment experiment = new UserExperiment('test', ['a'])

        when: String variant = experiment.chooseVariant(createRequestMock('path'))

        then: "a" == variant
    }

    protected HttpServletRequest createRequestMock(path) {
        def request = Mock(HttpServletRequest)
        request.getServletPath() >> { path }

        request
    }
}
