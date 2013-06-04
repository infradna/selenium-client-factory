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
package com.saucelabs.selenium.client.factory.spi;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

/**
 * SPI implemented by the {@link Selenium} and {@link WebDriver} implementation providers.
 *
 * <p>
 * Concrete implementations of this SPI must have a public no-argument constructor.
 * Instances of {@link SeleniumFactorySPI}s are discovered via /META-INF/services,
 * see <a href="http://download.oracle.com/docs/cd/E17476_01/javase/1.3/docs/guide/jar/jar.html#Service%20Provider">
 * the spec</a> for details of the service lookup.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SeleniumFactorySPI {
    /**
     * Instantiates the driver.
     *
     * <p>
     * This method is invoked in response to {@link SeleniumFactory#createSelenium()} to actually
     * instantiate the driver.
     *
     * @param factory
     *      The factory that captures the configuration that the calling user application is looking for.
     *      Never null.
     * @param browserURL
     *      See the parameter of the same name in {@link DefaultSelenium#DefaultSelenium(String, int, String, String)}.
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     *
     * @return
     *      null if the implementation didn't recognize the URI specified in the factory.
     *      returning null causes {@link SeleniumFactory} to try other SPIs found in the system.
     * @throws IllegalArgumentException
     *      If the URI was recognized by the SPI but some of its configurations were wrong,
     *      or if the SPI failed to instantiate the Selenium driver, throw an exception.
     *      {@link SeleniumFactory} will not try other SPIs and propagate the problem to the calling
     *      user application.
     */
    public abstract Selenium createSelenium(SeleniumFactory factory, String browserURL);

    /**
     * Instantiates the driver.
     *
     * <p>
     * This method is invoked in response to {@link com.saucelabs.selenium.client.factory.SeleniumFactory#createWebDriver()} ()} to actually
     * instantiate the driver.
     *
     * @param factory
     *      The factory that captures the configuration that the calling user application is looking for.
     *      Never null.
     * @param browserURL
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     * @param capabilities
     *      The capabilities to use, browser, browser version and os will be override.
     *
     * @return
     *      null if the implementation didn't recognize the URI specified in the factory.
     *      returning null causes {@link SeleniumFactory} to try other SPIs found in the system.
     * @throws IllegalArgumentException
     *      If the URI was recognized by the SPI but some of its configurations were wrong,
     *      or if the SPI failed to instantiate the Selenium driver, throw an exception.
     *      {@link SeleniumFactory} will not try other SPIs and propagate the problem to the calling
     *      user application.
     */
    public abstract WebDriver createWebDriver(SeleniumFactory factory,String browserURL, Capabilities capabilities);

    /**
     * Returns boolean indicating whether the Factory instance can handle the incoming URI.
     * @param uri URI to check
     * @return boolean that indicates whether the Factory can handle the incoming URI
     */
    public abstract boolean canHandle(String uri);
}
