package com.saucelabs.selenium.client.htmlunit;

import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

/**
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class HtmlUnitSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        if (factory.getUri().startsWith("htmlunit:"))
            return new SeleniumHTMLUnit();
        return null;
    }
}
