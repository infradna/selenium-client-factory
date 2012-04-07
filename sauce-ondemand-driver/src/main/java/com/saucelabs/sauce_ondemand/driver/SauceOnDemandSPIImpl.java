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
package com.saucelabs.sauce_ondemand.driver;

import com.saucelabs.rest.Credential;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.selenium.client.factory.spi.SeleniumFactorySPI;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * {@link SeleniumFactorySPI} that talks to Sauce OnDemand.
 * 
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class SauceOnDemandSPIImpl extends SeleniumFactorySPI {
    
    private static final String DEFAULT_WEBDRIVER_HOST = "ondemand.saucelabs.com";
    
    private static final int DEFAULT_WEBDRIVER_PORT = 80;
    
    private static final String DEFAULT_SELENIUM_HOST = "saucelabs.com";

    private static final int DEFAULT_SELENIUM_PORT = 4444;
    public static final String SELENIUM_HOST = "SELENIUM_HOST";
    public static final String SELENIUM_PORT = "SELENIUM_PORT";

    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!canHandle(uri))
            return null; // not ours

        uri = uri.substring(SCHEME.length());
        if (!uri.startsWith("?"))
            throw new IllegalArgumentException("Missing '?':"+factory.getUri());
        Map<String, List<String>> paramMap = populateParameterMap(uri);
        
        String host = readPropertyOrEnv("SELENIUM_HOST", DEFAULT_SELENIUM_HOST);
        String portAsString = readPropertyOrEnv("SELENIUM_PORT", null);
        int port;
        if (portAsString == null || portAsString.equals("")) {
            port = DEFAULT_SELENIUM_PORT;
        } else {
            port = Integer.parseInt(portAsString);
        }

        return new SeleniumImpl(
                host,port,
                toJSON(paramMap),
                browserURL,
                new Credential(paramMap.get("username").get(0), paramMap.get("access-key").get(0)),
                paramMap.get("job-name").get(0));
    }

    private Map<String, List<String>> populateParameterMap(String uri) {
        // massage parameter into JSON format
        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
        for (String param : uri.substring(1).split("&")) {
            int idx = param.indexOf('=');
            if(idx<0)   throw new IllegalArgumentException("Invalid parameter format: "+uri);
            String key = param.substring(0,idx);
            String value = param.substring(idx+1);

            List<String> v = paramMap.get(key);
            if (v==null)    paramMap.put(key, v = new ArrayList<String>());
            v.add(value);
        }

        if (paramMap.get("username")==null && paramMap.get("access-key")==null) {
            try {
                // read the credential from a credential file
                Credential cred = new Credential();
                paramMap.put("username", Collections.singletonList(cred.getUsername()));
                paramMap.put("access-key", Collections.singletonList(cred.getKey()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read "+Credential.getDefaultCredentialFile(),e);
            }
        }

        if (paramMap.get("job-name")==null)
            paramMap.put("job-name",Collections.singletonList(getJobName()));
        return paramMap;
    }

    @Override
    public WebDriver createWebDriver(SeleniumFactory factory, String browserURL) {

        String uri = factory.getUri();
        if (!uri.startsWith(SCHEME))
            return null; // not ours

        uri = uri.substring(SCHEME.length());
        if (!uri.startsWith("?"))
            throw new IllegalArgumentException("Missing '?':"+factory.getUri());

        // massage parameter into JSON format
        Map<String, List<String>> paramMap = populateParameterMap(uri);

        DesiredCapabilities desiredCapabilities;
        if (hasParameter(paramMap, "os") &&
                hasParameter(paramMap,"browser") &&
                hasParameter(paramMap, "browser-version")) {
            desiredCapabilities = new DesiredCapabilities(
                    getFirstParameter(paramMap, "browser"),
                    getFirstParameter(paramMap, "browser-version"),
                    Platform.extractFromSysProperty(getFirstParameter(paramMap, "os")));
        } else {
            //use Firefox as a default
            desiredCapabilities = DesiredCapabilities.firefox();
        }
        String host = readPropertyOrEnv(SELENIUM_HOST, DEFAULT_WEBDRIVER_HOST);

        String portAsString = readPropertyOrEnv(SELENIUM_PORT, null);
        int port;
        if (portAsString == null || portAsString.equals("")) {
            port = DEFAULT_WEBDRIVER_PORT;
        } else {
            port = Integer.parseInt(portAsString);
        }

        try {
            WebDriver driver = new RemoteWebDriverImpl(
                    new URL(
                            MessageFormat.format(
                                    "http://{2}:{3}@{0}:{1}/wd/hub",
                                    host, port,
                                    getFirstParameter(paramMap,"username"),
                                    getFirstParameter(paramMap,"access-key"))),
                            desiredCapabilities,
                    new Credential(getFirstParameter(paramMap,"username"), getFirstParameter(paramMap,"access-key")),
                    getFirstParameter(paramMap,"job-name"));

            driver.get(browserURL);
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: "+factory.getUri(),e);
        }
    }

    @Override
    public boolean canHandle(String uri) {
        return uri.startsWith(SCHEME);
    }

    /**
     * Try to find the name of the test as best we can.
     */
    public String getJobName() {
        // look for the caller of SeleniumFactory
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        boolean foundFactory = false;
        String callerName = null;
        for (StackTraceElement e : trace) {
            if (foundFactory)
                callerName = e.getClassName() + "." + e.getMethodName();
            foundFactory = e.getClassName().equals(SeleniumFactory.class.getName());
        }
        return callerName;
    }

    /**
     * Converts a multi-map to a JSON format.
     */
    private String toJSON(Map<String, List<String>> paramMap) {
        boolean first=true;
        StringBuilder buf = new StringBuilder("{");
        for (Entry<String, List<String>> e : paramMap.entrySet()) {
            if (first)  first = false;
            else        buf.append(',');
            buf.append('"').append(e.getKey()).append("\":");

            List<String> v = e.getValue();
            if (v.size()==1) {
                buf.append('"').append(v.get(0)).append('"');
            } else {
                buf.append('[');
                for (int i=0; i<v.size(); i++) {
                    if(i!=0)    buf.append(',');
                    buf.append('"').append(v.get(i)).append('"');
                }
                buf.append(']');
            }
        }
        buf.append('}');
        return buf.toString();
    }

    private String getFirstParameter(Map<String, List<String>> paramMap, String parameterName) {
        List<String> values = paramMap.get(parameterName);
        return values.get(0);
    }

    private boolean hasParameter(Map<String, List<String>> paramMap, String parameterName) {
        List<String> values = paramMap.get(parameterName);
        return values != null && !values.isEmpty();
    }

    private static final String SCHEME = "sauce-ondemand:";

    private static String readPropertyOrEnv(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v==null)
            v = System.getenv(key);
        if (v==null)
            v = defaultValue;
        return v;
    }
}
