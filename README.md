# Mitosis ![](https://circleci.com/gh/transferwise/mitosis/tree/master.svg?style=shield&circle-token=e4edd781ef41072d705e58afe8fe17b79a9d4743)

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
    compile 'com.github.transferwise:mitosis:1.0.0'
}
```

## Configuration

Integration with Spring.

```java
@Bean
public Filter experimentFilter() {
    ExperimentFilter filter = new ExperimentFilter(3600 * 24 * 14, "ab", "experiments", "activate");

    filter.prepare("test", Arrays.asList("a", "b"));

    return filter;
}
```

## Usage

In Thymeleaf templates.

```html
<div th:if="${experiments['test'] == 'a'}">
    This is the A version
</div>

<div th:if="${experiments['test'] == 'b'}">
    This is the B version
</div>
```

## Advanced use

You can use lambda functions tu create more complex filters, so you will split your traffic only under controled circunstances.
Let's imagine you want to run your test only for people visiting some urls starting with "/path":


```java 
@Bean
public Filter experimentFilter() {
    ExperimentFilter filter = new ExperimentFilter(3600 * 24 * 14, "ab", "experiments", "activate");

    filter.prepare("test", Arrays.asList("a", "b"), r -> r.getRequestURI().contains("/path"));

    return filter;
}

``` 
 
When using lambda functions, the experiment variant will be assigned only if the function returns `true`. In other case no variant will be assigned, so you need to handle within your logic.
