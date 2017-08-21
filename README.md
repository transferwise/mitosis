# Mitosis [![CircleCI](https://circleci.com/gh/transferwise/mitosis/tree/master.svg?style=shield)](https://circleci.com/gh/transferwise/mitosis/tree/master)

A/B traffic split servlet filter.

## Installation

Just add the following configuration to your `build.gradle` file

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    compile 'com.github.transferwise:mitosis:1.0.1'
}
```

## Configuration

If we wanted to [uniformly distribute](https://en.wikipedia.org/wiki/Uniform_distribution_(continuous)) `a` and `b` variants for an experiment `test` across the requests of our application, the configuration would be

```java
@Bean
public Filter experimentFilter() {
    ExperimentFilter filter = new ExperimentFilter(3600 * 24 * 14, "ab", "experiments", "activate");

    filter.prepare("test", asList("a", "b"));

    return filter;
}
```

## Usage

In Spring controllers.

```java
@RequestMapping("/do-something")
public String doSomething(@RequestAttribute("experiments") Map<String, String> experiments) {
    if (experiments.get("test").equals("a")) {
        return goForVariantA();
    }
    if (experiments.get("test").equals("b")) {
        return goForVariantB();
    }
}
```

In Thymeleaf templates.

```html
<div th:if="${experiments['test'] == 'a'}">
    This is the A version
</div>

<div th:if="${experiments['test'] == 'b'}">
    This is the B version
</div>

<script th:inline="javascript">
    var experiments = [[${experiments}]];
    if (experiments['test'] === 'a') {
        goForVariantA();
    }
    if (experiments['test'] === 'b') {
        goForVariantB();
    }
</script>
```

## Forcing an experiment variant

Variants are uniformly distributed automatically. A way of forcing a specific experiment variant, so you can share and preview the experiment, is to pass the preferred variant as a request parameter.

In an experiment configuration like the following

```java
@Bean
public Filter experimentFilter() {
    ExperimentFilter filter = new ExperimentFilter(3600 * 24 * 14, "ab", "experiments", "activate");

    filter.prepare("test1", asList("a", "b"));
    filter.prepare("test2", asList("c", "d"));

    return filter;
}
```

You can force the test `test1` to variant `b` by setting the `activate` query parameter in the following way

    https://yoursite.com/?activate=test1:b

Worth mentioning that if you have multiple experiments running and you want to force multiple variants you can do

    https://yoursite.com/?activate=test1:b,test2:c

## Advanced use

You can use lambda functions tu create more complex filters, so you will split your traffic only under controled circunstances.
Let's imagine you want to run your test only for people visiting some urls starting with "/path":


```java 
@Bean
public Filter experimentFilter() {
    ExperimentFilter filter = new ExperimentFilter(3600 * 24 * 14, "ab", "experiments", "activate");

    filter.prepare("test", asList("a", "b"), r -> r.getRequestURI().contains("/path"));

    return filter;
}

``` 
 
When using lambda functions, the experiment variant will be assigned only if the function returns `true`. **Otherwise, no variant will be assigned**, so you need to handle within your logic.

Mitosis provides a handful request filters that can be combined at will. If we wanted to assign variants only to english-speaking users, excluding Googlebot crawler so our experiment won't affect SEO, we can configure the experiment in the following way

```java
filter.prepare("test", asList("a", "b"), languageEquals("en").and(userAgentContains("googlebot").negate()));
```
