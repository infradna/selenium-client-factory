package org.seleniumhq.selenium.client.logging;

import com.thoughtworks.selenium.Selenium;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

import java.lang.reflect.Proxy;

/**
 * @author Kohsuke Kawaguchi
 */
public class LoggingSeleniumSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!uri.startsWith("log:"))       return null;    // not our URL

        Selenium base = factory.clone().setUri(uri.substring(4)).createSelenium(browserURL);
        return createLoggingSelenium(base);
    }

    /**
     * Creates a logging selenium around the given Selenium driver.
     */
    public static Selenium createLoggingSelenium(Selenium base) {
        return (Selenium) Proxy.newProxyInstance(LoggingSelenium.class.getClassLoader(),
                new Class[]{LoggingSelenium.class, Selenium.class},
                new LoggingSeleniumProxy(base));
    }
}
