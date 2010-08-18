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
package com.saucelabs.selenium.client.factory.impl;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.selenium.client.factory.spi.SeleniumFactorySPI;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * {@link SeleniumFactorySPI} that connects to Selenium RCs over its standard HTTP-based protocol.
 *
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class DefaultSeleniumSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory,String browserURL) {
        if (!factory.getUri().startsWith("http:"))  return null;    // doesn't belong to us

        try {
            URL url = new URL(factory.getUri());

            int p = url.getPort();
            if (p==-1)  p = DEFAULT_PORT;

            // since the browser start command parameter can contain arbitrary character that may
            // potentially interfere with the rules of the URL, allow the user to specify it through a property.
            String browserStartCommand = (String)factory.getProperty("browserStartCommand");
            if (browserStartCommand==null)
                // getPath starts with '/', so trim it off
                // do URL decode, so that arbitrary strings can be passed in.
                browserStartCommand = URLDecoder.decode(url.getPath().substring(1), "UTF-8");

            return new DefaultSelenium(url.getHost(), p, browserStartCommand, browserURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: "+factory.getUri(),e);
        } catch (UnsupportedEncodingException e) {
            // impossible
            throw new Error(e);
        }
    }

    public static final int DEFAULT_PORT = Integer.getInteger(DefaultSeleniumSPIImpl.class.getName()+".defaultPort",4444);
}
