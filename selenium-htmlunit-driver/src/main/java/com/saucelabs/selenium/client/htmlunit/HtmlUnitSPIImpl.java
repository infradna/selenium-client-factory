/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saucelabs.selenium.client.htmlunit;

import org.kohsuke.MetaInfServices;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.selenium.client.factory.spi.SeleniumFactorySPI;
import com.thoughtworks.selenium.Selenium;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class HtmlUnitSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        if (canHandle(factory.getUri()))
            return new SeleniumHTMLUnit();
        return null;
    }


    @Override
    public WebDriver createWebDriver(SeleniumFactory factory, String browserURL, DesiredCapabilities capabilities) {
        if (canHandle(factory.getUri())) {
            WebDriver driver = new HtmlUnitDriver();
            driver.get(browserURL);
            return driver;
        }
        return null;
    }

    @Override
    public List<WebDriver> createWebDrivers(SeleniumFactory seleniumFactory, String browserURL) {
        throw new IllegalArgumentException("Not Supported");
    }

    @Override
    public boolean canHandle(String uri) {
        return uri.startsWith("htmlunit:");
    }
}
