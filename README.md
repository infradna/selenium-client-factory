This project contains the source code for the Selenium Client Factory library, which simplifies the usage  and integration with the Sauce OnDemand plugin CI plugins.

It allows you to construct WebDriver and SeleniumRC instances in a single line, as the factory implementation will handle referencing the environment variables and system properties set by the CI plugin, eg.

```java
WebDriver webDriver = SeleniumFactory.createWebDriver();
```

To reference the library, include the following dependencies in your Maven project

```xml
<dependency>
    <groupId>com.saucelabs.selenium</groupId>
    <artifactId>sauce-ondemand-driver</artifactId>
    <version>2.13</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.saucelabs.selenium</groupId>
    <artifactId>selenium-client-factory</artifactId>
    <version>2.13</version>
    <scope>test</scope>
</dependency>
```
You will also need to reference the Sauce Labs Maven repository

```xml
<repositories>
    <repository>
        <id>saucelabs-repository</id>
        <url>https://repository-saucelabs.forge.cloudbees.com/release</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```
