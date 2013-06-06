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
package com.saucelabs.selenium.client.factory;

import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.saucelabs.selenium.client.factory.spi.SeleniumFactorySPI;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

/**
 * Factory of {@link Selenium}.
 *
 * <p>
 * Compared to directly initializing {@link com.thoughtworks.selenium.DefaultSelenium}, this additional indirection
 * allows the build script or a CI server to control how you connect to the selenium.
 * This makes it easier to run the same set of tests in different environments without
 * modifying the test code.
 *
 * <p>
 * This is analogous to how you connect to JDBC &mdash; you normally don't directly
 * instantiate a specific driver, and instead you do {@link DriverManager#getConnection(String)}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SeleniumFactory {
    /**
     * Uses a driver specified by the 'SELENIUM_DRIVER' system property or the environment variable,
     * and run the test against the domain specified in 'SELENIUM_STARTING_URL' system property or the environment variable.
     *
     * <p>
     * If exists, the system property takes precedence over the environment variable.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().createSelenium()}.
     */
    public static Selenium create() {
        return new SeleniumFactory().createSelenium();
    }

    /**
     * Uses a driver specified by the 'SELENIUM_DRIVER' system property or the environment variable,
     * and run the test against the domain specified in 'SELENIUM_STARTING_URL' system property or the environment variable.
     *
     * <p>
     * If exists, the system property takes precedence over the environment variable.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().createWebDriver()}.
     */
    public static WebDriver createWebDriver() {
        return new SeleniumFactory().createWebDriverInstance(null);
    }

    /**
     * Uses a driver specified by the 'SELENIUM_DRIVER' system property or the environment variable,
     * and run the test against the domain specified in 'SELENIUM_STARTING_URL' system property or the environment variable.
     *
     * <p>
     * If exists, the system property takes precedence over the environment variable.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().createWebDriver()}.
     * 
     * @param capabilities
     *      The desired driver capabilities to use, browser, browser version and os will be override.
     */
    public static WebDriver createWebDriver(DesiredCapabilities capabilities) {
        return new SeleniumFactory().createWebDriverInstance(capabilities);
    }

    /**
     * Uses a driver specified by the 'SELENIUM_DRIVER' system property or the environment variable,
     * and run the test against the specified domain.
     *
     * <p>
     * If exists, the system property takes precedence over the environment variable.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().createSelenium(browserURL)}.
     *
     * @param browserURL
     *      See the parameter of the same name in {@link DefaultSelenium#DefaultSelenium(String, int, String, String)}.
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     */
    public static Selenium create(String browserURL) {
        return new SeleniumFactory().createSelenium(browserURL);
    }

    /**
     * Uses the specified driver and the test domain and create a driver instance.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().setUri(driverUri).createSelenium(browserURL)}.
     *
     * @param driverUri
     *      The URI indicating the Selenium driver to be instantiated.
     * @param browserURL
     *      See the parameter of the same name in {@link DefaultSelenium#DefaultSelenium(String, int, String, String)}.
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     */
    public static Selenium create(String driverUri, String browserURL) {
        return new SeleniumFactory().setUri(driverUri).createSelenium(browserURL);
    }

    /**
     * Uses the specified driver and the test domain and create a WebDriver instance.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().setUri(driverUri).createSelenium(browserURL)}.
     *
     * @param driverUri
     *      The URI indicating the Selenium driver to be instantiated.
     * @param browserURL
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     */
    public static WebDriver createWebDriver(String driverUri, String browserURL) {
        return new SeleniumFactory().setUri(driverUri).createWebDriverInstance(browserURL, null);
    }

    /**
     * Uses the specified driver and the test domain and create a WebDriver instance.
     *
     * <p>
     * This is just a convenient short-cut for {@code new SeleniumFactory().setUri(driverUri).createSelenium(browserURL)}.
     *
     * @param driverUri
     *      The URI indicating the Selenium driver to be instantiated.
     * @param browserURL
     *      This specifies the domain name in the format of "http://foo.example.com" where the test occurs.
     * @param capabilities
     *      The desired driver capabilities to use, browser, browser version and os will be override.
     */
    public static WebDriver createWebDriver(String driverUri, String browserURL, DesiredCapabilities capabilities) {
        return new SeleniumFactory().setUri(driverUri).createWebDriverInstance(browserURL, capabilities);
    }

    private String uri;
    private ClassLoader cl = Thread.currentThread().getContextClassLoader();
    private Map<String,Object> properties = new HashMap<String,Object>();

    public SeleniumFactory() {
        // use the embedded RC as the default, since this is the least environment dependent.
        uri = readPropertyOrEnv("SELENIUM_DRIVER",readPropertyOrEnv("DEFAULT_SELENIUM_DRIVER","embedded-rc:"));
    }

    private static String readPropertyOrEnv(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v==null)
            v = System.getenv(key);
        if (v==null)
            v = defaultValue;
        return v;
    }

    /**
     * Gets the driver URI set by the {@link #setUri(String)}
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the URI of the Selenium driver.
     * <p>
     * Initially, the value of the 'SELENIUM_DRIVER' system property of the environment variable is read
     * and set. The system property takes precedence over the environment variable.
     *
     * @return
     *      'this' instance to facilitate the fluent API pattern.
     */
    public SeleniumFactory setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Gets the classloader set by the {@link #setClassLoader(ClassLoader)}.
     */
    public ClassLoader getClassLoader() {
        return cl;
    }

    /**
     * Sets the classloader used for searching the driver.
     * Initially set to {@code Thread.currentThread().getContextClassLoader()} of the thread
     * that instantiated this factory.
     *
     * @return
     *      'this' instance to facilitate the fluent API pattern.
     */
    public SeleniumFactory setClassLoader(ClassLoader cl) {
        this.cl = cl;
        return this;
    }

    /**
     * Sets other misc. driver-specific properties. Refer to the driver implementation
     * for the valid properties and their expected types.
     *
     * @return
     *      'this' instance to facilitate the fluent API pattern.
     */
    public SeleniumFactory setProperty(String key, Object value) {
        this.properties.put(key,value);
        return this;
    }

    /**
     * Retrieves the value of the property previously set.
     */
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    /**
     * Returns the live map that stores the property values.
     * Convenient for bulk update operations.
     *
     * @return never null
     */
    public Map<String,Object> getProperties() {
        return this.properties;
    }

    /**
     * Creates a clone of this factory that's identically configured.
     * <p>
     * Properties are only shallowly copied.
     */
    public SeleniumFactory clone() {
        SeleniumFactory f = new SeleniumFactory();
        f.uri = uri;
        f.cl = cl;
        f.properties.clear();
        f.properties.putAll(properties);
        return f;
    }

    /**
     * Based on the current configuration, instantiate a Selenium driver
     * and returns it.
     *
     * <p>
     * This version implicitly retrieves the 'browserURL' parameter and
     * calls into {@link #createSelenium(String)} by checking the 'SELENIUM_STARTING_URL'
     * system property or the environment variable. The system property takes precedence over the environment variable.
     *
     * @throws IllegalArgumentException
     *      if the configuration is invalid, or the driver failed to instantiate.
     * @return never null
     */
    public Selenium createSelenium() {
        String url = readPropertyOrEnv("SELENIUM_STARTING_URL",readPropertyOrEnv("DEFAULT_SELENIUM_STARTING_URL",null));
        if (url==null)
            throw new IllegalArgumentException("Neither SELENIUM_STARTING_URL/DEFAULT_SELENIUM_STARTING_URL system property nor environment variable exists");
        return createSelenium(url);
    }

    /**
     * Based on the current configuration, instantiate a Selenium driver
     * and returns it.
     *
     * <p>
     * This version implicitly retrieves the 'browserURL' parameter and
     * calls into {@link #createSelenium(String)} by checking the 'SELENIUM_STARTING_URL'
     * system property or the environment variable. The system property takes precedence over the environment variable.
     * 
     * @param capabilities
     *      The desired driver capabilities to use, browser, browser version and os will be override.
     * 
     * @throws IllegalArgumentException
     *      if the configuration is invalid, or the driver failed to instantiate.
     * @return never null
     */
    public WebDriver createWebDriverInstance(DesiredCapabilities capabilities) {
        String url = readPropertyOrEnv("SELENIUM_STARTING_URL",readPropertyOrEnv("DEFAULT_SELENIUM_STARTING_URL",null));
        return createWebDriverInstance(url, capabilities);
    }

    public Selenium createSelenium(String browserURL) {
        SeleniumFactorySPI seleniumFactory = createSeleniumFactory();
        Selenium selenium = seleniumFactory.createSelenium(this, browserURL);
        if (selenium == null) {
            throw new IllegalArgumentException(String.format(
                    "Unrecognized Selenium driver URI '%s'. Make sure you got the proper driver jars in your classpath, or increase the logging level to get more information.", uri));
        } else {
            return selenium;
        }
    }

    public WebDriver createWebDriverInstance(String browserURL, DesiredCapabilities capabilities) {
        SeleniumFactorySPI seleniumFactory = createSeleniumFactory();
        WebDriver webDriver = seleniumFactory.createWebDriver(this, browserURL, capabilities);
        if (webDriver == null) {
            throw new IllegalArgumentException(String.format(
                    "Unrecognized Selenium driver URI '%s'. Make sure you got the proper driver jars in your classpath, or increase the logging level to get more information.", uri));
        } else {
            return webDriver;
        }
    }

    /**
     * Based on the current configuration, instantiate a Selenium driver
     * and returns it.
     *
     * @throws IllegalArgumentException
     *      if the configuration is invalid, or the driver failed to instantiate.
     * @return never null
     */
    private SeleniumFactorySPI createSeleniumFactory() {
        try {
            if (uri==null)
                throw new IllegalArgumentException("Selenium driver URI is not set");

            Enumeration<URL> e = cl.getResources("META-INF/services/" + SeleniumFactorySPI.class.getName());
            SeleniumFactorySPI seleniumFactory = null;
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                LOGGER.fine("Reading "+url+" looking for "+SeleniumFactorySPI.class.getName());
                BufferedReader in = new LineNumberReader(new InputStreamReader(url.openStream(),"UTF-8"));
                try {
                    String line;
                    while ((line=in.readLine())!=null) {
                        line = line.trim();
                        if (line.startsWith("#"))   continue;   // comment

                        // otherwise treat this as FQCN
                        LOGGER.fine("Found "+line);
                        try {
                            Class<?> c = cl.loadClass(line);
                            LOGGER.fine("Loaded "+c);

                            Object _spi = c.newInstance();
                            boolean canHandleRequest = false;
                            if (_spi instanceof SeleniumFactorySPI) {
                                seleniumFactory = (SeleniumFactorySPI) _spi;
                                if (seleniumFactory.canHandle(uri)) {
                                    return seleniumFactory;
                                }
                            }
                            if (!canHandleRequest) {
                                URL img = c.getClassLoader().getResource(SeleniumFactorySPI.class.getName().replace('.','/')+".class");
                                LOGGER.log(WARNING, url+" specifies an SPI class "+line+" but the class isn't assignable to "+SeleniumFactorySPI.class+". It's loading SPI from "+img);
                            }
                        } catch (ClassNotFoundException x) {
                            LOGGER.log(WARNING, url+" specifies an SPI class "+line+" but the class failed to load",x);
                        } catch (InstantiationException x) {
                            LOGGER.log(WARNING, url+" specifies an SPI class "+line+" but the class failed to instantiate",x);
                        } catch (IllegalAccessException x) {
                            LOGGER.log(WARNING, url+" specifies an SPI class "+line+" but the class failed to instantiate",x);
                        }

                    }
                } finally {
                    in.close();
                }
            }

            if (seleniumFactory == null) {
                throw new IllegalArgumentException(String.format(
                    "Unrecognized Selenium driver URI '%s'. Make sure you got the proper driver jars in your classpath, or increase the logging level to get more information.", uri));
            } else {
                return seleniumFactory;
            }

        } catch (IOException x) {
            throw new IllegalArgumentException("Failed to instantiate the driver",x);
        }

    }

    private static final Logger LOGGER = Logger.getLogger(SeleniumFactory.class.getName());

}
