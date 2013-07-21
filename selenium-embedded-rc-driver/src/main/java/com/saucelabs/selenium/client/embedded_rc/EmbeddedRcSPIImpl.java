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
package com.saucelabs.selenium.client.embedded_rc;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.selenium.client.factory.spi.SeleniumFactorySPI;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.cli.RemoteControlLauncher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

/**
 * {@link SeleniumFactorySPI} implementation that lets you run Selenium RC inside the same JVM.
 *
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class EmbeddedRcSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!canHandle(uri))        return null;    // not ours
        String browser = uri.substring(SCHEME.length());

        if (browser.length()==0)
            browser = getPlatformDefaultBrowser();

        int port = allocateRandomPort();
        final SeleniumServer server = startSeleniumServer(factory, port);

        // create a normal client driver
        SeleniumFactory f = factory.clone();
        Selenium base = f.setUri("http://localhost:" + port + "/" + browser).createSelenium(browserURL);

        // if the selenium session is shut down, stop the embedded RC
        return new SeleniumFilter(base) {
            @Override
            public void stop() {
                try {
                    super.stop();
                } finally {
                    server.stop();
                }
            }
        };
    }

      private SeleniumServer startSeleniumServer(SeleniumFactory factory, int port) {
        // allow the additional parameters to be passed in.
        String[] args = getArguments(factory);
        RemoteControlConfiguration configuration = RemoteControlLauncher.parseLauncherOptions(args);
        configuration.setPort(port);

        if (System.getProperty(JETTY_FORM_SIZE)==null)
            System.setProperty(JETTY_FORM_SIZE, "0");

        final SeleniumServer server;
        try {
            server = new SeleniumServer(Boolean.getBoolean("slowResources"), configuration);
            // TODO: it'd be nice if SeleniumServer can be configured to listen only on 127.0.0.1 for security reasons
            server.boot();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to start embedded RC",e);
        }
        return server;
    }

    @Override
    public WebDriver createWebDriver(SeleniumFactory factory, String browserURL, DesiredCapabilities capabilities) {
        String uri = factory.getUri();
        if (!canHandle(uri))        return null;    // not ours
        String browser = uri.substring(SCHEME.length());

        if (browser.length()==0)
            browser = getPlatformDefaultBrowser();

        int port = allocateRandomPort();
        final SeleniumServer server = startSeleniumServer(factory, port);

        // create a normal client driver
        SeleniumFactory f = factory.clone();
        WebDriver base = f.setUri("http://localhost:" + port + "/wd/hub").createWebDriverInstance(browserURL, capabilities);


        // if the selenium session is shut down, stop the embedded RC
        return new WebDriverFilter(base) {
            @Override
            public void quit() {
                try {
                    super.quit();
                } finally {
                    server.stop();
                }
            }

        };
    }

    @Override
    public boolean canHandle(String uri) {
        return uri.startsWith(SCHEME);
    }

    private String[] getArguments(SeleniumFactory factory) {
        Object ea = factory.getProperty("embedded_args");
        if (ea instanceof String)
            return ((String) ea).split(" +");

        String[] args = (String[]) ea;
        if (args==null) args = new String[0];
        return args;
    }

    /**
     * allocate a random port
     * this is less than ideal, in that there's a possibility that the random port assigned by this
     * will be occupied by someone else before Selenium server starts, but it doesn't look like
     * Selenium server currently supports dynamic port allocation.
     */
    private int allocateRandomPort() {
        try {
            ServerSocket ss = new ServerSocket(0);
            int port = ss.getLocalPort();
            ss.close();
            return port;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to allocate a random port",e);
        }
    }

    protected String getPlatformDefaultBrowser() {
        if (File.pathSeparatorChar==';')
            return "*iexplore";

        String osName = System.getProperty("os.name");
        if (osName.contains("Mac") || osName.startsWith("Darwin"))
            return "*safari";

        return "*firefox";
    }

    @Override
    public List<WebDriver> createWebDrivers(SeleniumFactory seleniumFactory, String browserURL) {
        throw new IllegalArgumentException("Not Supported");
    }

    private static final String SCHEME = "embedded-rc:";

    private static final String JETTY_FORM_SIZE = "org.openqa.jetty.http.HttpRequest.maxFormContentSize";
}
