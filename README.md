# Mitosis

A/B traffic split servlet filter.

## Installation

Add the dependency in your gradle file.

```groovy
repositories {
    maven {
        url 'https://dl.bintray.com/transferwise/maven'
    }
}

dependencies {
    compile 'com.transferwise:mitosis:1.0.0'
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