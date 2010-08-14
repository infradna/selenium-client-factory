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
package com.saucelab.selenium.client.client.factory.spi;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import com.saucelab.selenium.client.client.factory.SeleniumFactory;

/**
 * SPI implemented by the {@link Selenium} driver implementation providers.
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
}
