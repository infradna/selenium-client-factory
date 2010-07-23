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
