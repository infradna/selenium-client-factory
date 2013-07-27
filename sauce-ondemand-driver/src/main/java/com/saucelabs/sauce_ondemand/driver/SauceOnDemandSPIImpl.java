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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.kohsuke.MetaInfServices;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * {@link SeleniumFactorySPI} that talks to Sauce OnDemand.
 *
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class SauceOnDemandSPIImpl extends SeleniumFactorySPI {

    private static final String DEFAULT_WEBDRIVER_HOST = "ondemand.saucelabs.com";

    private static final String DEFAULT_WEBDRIVER_PORT = "80";

    private static final String DEFAULT_SELENIUM_HOST = "saucelabs.com";

    private static final int DEFAULT_SELENIUM_PORT = 4444;
    public static final String SELENIUM_HOST = "SELENIUM_HOST";
    public static final String SELENIUM_PORT = "SELENIUM_PORT";
    public static final String OS = "os";
    public static final String BROWSER = "browser";
    public static final String BROWSER_VERSION = "browser-version";
    public static final String USERNAME = "username";
    public static final String ACCESS_KEY = "access-key";

    private static final String[] NON_PROFILE_PARAMETERS = new String[]{ACCESS_KEY, BROWSER, BROWSER_VERSION, OS, USERNAME};

    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!canHandle(uri))
            return null; // not ours

        uri = uri.substring(SCHEME.length());
        if (!uri.startsWith("?"))
            throw new IllegalArgumentException("Missing '?':" + factory.getUri());
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
                host, port,
                toJSON(paramMap),
                browserURL,
                new Credential(paramMap.get(USERNAME).get(0), paramMap.get(ACCESS_KEY).get(0)),
                paramMap.get("job-name").get(0));
    }

    private Map<String, List<String>> populateParameterMap(String uri) {
        // massage parameter into JSON format
        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
        for (String param : uri.substring(1).split("&")) {
            int idx = param.indexOf('=');
            if (idx < 0) throw new IllegalArgumentException("Invalid parameter format: " + uri);
            String key = param.substring(0, idx);
            String value = param.substring(idx + 1);

            List<String> v = paramMap.get(key);
            if (v == null) paramMap.put(key, v = new ArrayList<String>());
            v.add(value);
        }

        if (paramMap.get(USERNAME) == null && paramMap.get(ACCESS_KEY) == null) {
            try {
                // read the credential from a credential file
                Credential cred = new Credential();
                paramMap.put(USERNAME, Collections.singletonList(cred.getUsername()));
                paramMap.put(ACCESS_KEY, Collections.singletonList(cred.getKey()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read " + Credential.getDefaultCredentialFile(), e);
            }
        }

        if (paramMap.get("job-name") == null)
            paramMap.put("job-name", Collections.singletonList(getJobName()));
        return paramMap;
    }

    @Override
    public WebDriver createWebDriver(SeleniumFactory factory, String browserURL, DesiredCapabilities capabilities) {

        String uri = factory.getUri();
        if (!uri.startsWith(SCHEME))
            return null; // not ours

        uri = uri.substring(SCHEME.length());
        if (!uri.startsWith("?"))
            throw new IllegalArgumentException("Missing '?':" + factory.getUri());
        return createWebDriver(browserURL, capabilities, uri);

    }

    private WebDriver createWebDriver(String browserURL, DesiredCapabilities capabilities, String uri) {
        // massage parameter into JSON format
        Map<String, List<String>> paramMap = populateParameterMap(uri);

        DesiredCapabilities desiredCapabilities;
        if (hasParameter(paramMap, OS) &&
                hasParameter(paramMap, BROWSER) &&
                hasParameter(paramMap, BROWSER_VERSION)) {
            String browser = getFirstParameter(paramMap, BROWSER);
            desiredCapabilities = new DesiredCapabilities(capabilities);
            desiredCapabilities.setBrowserName(browser);
            desiredCapabilities.setVersion(getFirstParameter(paramMap, BROWSER_VERSION));
            desiredCapabilities.setCapability(CapabilityType.PLATFORM, getFirstParameter(paramMap, OS));
            if (browser.equals("firefox")) {
                setFirefoxProfile(paramMap, desiredCapabilities);
            }
            populateDesiredCapabilities(paramMap, desiredCapabilities);
        } else {
            //use Firefox as a default
            desiredCapabilities = capabilities;
            desiredCapabilities.merge(DesiredCapabilities.firefox());
            setFirefoxProfile(paramMap, desiredCapabilities);
        }
        String host = readPropertyOrEnv(SELENIUM_HOST, DEFAULT_WEBDRIVER_HOST);

        String portAsString = readPropertyOrEnv(SELENIUM_PORT, null);

        if (portAsString == null || portAsString.equals("")) {
            portAsString = DEFAULT_WEBDRIVER_PORT;
        }

        try {
            WebDriver driver = new RemoteWebDriverImpl(
                    new URL(
                            MessageFormat.format(
                                    "http://{2}:{3}@{0}:{1}/wd/hub",
                                    host, portAsString,
                                    getFirstParameter(paramMap, USERNAME),
                                    getFirstParameter(paramMap, ACCESS_KEY))),
                    desiredCapabilities,
                    new Credential(getFirstParameter(paramMap, USERNAME), getFirstParameter(paramMap, ACCESS_KEY)),
                    getFirstParameter(paramMap, "job-name"));

            if (browserURL != null) {
                driver.get(browserURL);
            }
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + uri, e);
        }
    }

    private void populateDesiredCapabilities(Map<String, List<String>> paramMap, DesiredCapabilities desiredCapabilities) {
        for (Entry<String, List<String>> entry : paramMap.entrySet()) {
            desiredCapabilities.setCapability(entry.getKey(), entry.getValue().get(0));
        }
    }

    private void setFirefoxProfile(Map<String, List<String>> paramMap, DesiredCapabilities desiredCapabilities) {
        FirefoxProfile profile = new FirefoxProfile();
        populateProfilePreferences(profile, paramMap);
        desiredCapabilities.setCapability("firefox_profile", profile);
    }

    private void populateProfilePreferences(FirefoxProfile profile, Map<String, List<String>> paramMap) {
        for (Map.Entry<String, List<String>> mapEntry : paramMap.entrySet()) {
            String key = mapEntry.getKey();
            if (Arrays.binarySearch(NON_PROFILE_PARAMETERS, key) == -1) {
                //add it to the profile
                profile.setPreference(key, getFirstParameter(paramMap, key));
            }
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
        boolean first = true;
        StringBuilder buf = new StringBuilder("{");
        for (Entry<String, List<String>> e : paramMap.entrySet()) {
            if (first) first = false;
            else buf.append(',');
            buf.append('"').append(e.getKey()).append("\":");

            List<String> v = e.getValue();
            if (v.size() == 1) {
                buf.append('"').append(v.get(0)).append('"');
            } else {
                buf.append('[');
                for (int i = 0; i < v.size(); i++) {
                    if (i != 0) buf.append(',');
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
        if (v == null)
            v = System.getenv(key);
        if (v == null)
            v = defaultValue;
        return v;
    }

    /**
     * Creates a list of WebDriver instances based on the contents of a SAUCE_ONDEMAND_BROWSERS environment variable (typically set
     * by the Sauce Jenkins plugin).
     *
     * @param seleniumFactory
     * @param browserURL
     * @return
     */
    @Override
    public List<WebDriver> createWebDrivers(SeleniumFactory seleniumFactory, final String browserURL) {

        List<WebDriver> webDrivers = new ArrayList<WebDriver>();
        String browserJson = readPropertyOrEnv("SAUCE_ONDEMAND_BROWSERS", null);
        if (browserJson == null) {
            throw new IllegalArgumentException("Unable to find SAUCE_ONDEMAND_BROWSERS environment variable");
        }
        //parse JSON and extract the browser urls, so that we know how many threads to schedule
        List<String> browsers = new ArrayList<String>();
        try {
            JSONArray array = new JSONArray(new JSONTokener(browserJson));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String uri = object.getString("url");
                if (!uri.startsWith(SCHEME))
                    return null; // not ours

                uri = uri.substring(SCHEME.length());
                if (!uri.startsWith("?"))
                    throw new IllegalArgumentException("Missing '?':" + uri);
                browsers.add(uri);
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error parsing JSON", e);
        }

        //create a fixed thread pool for the number of browser
        ExecutorService service = Executors.newFixedThreadPool(browsers.size());
        List<Callable<WebDriver>> callables = new ArrayList<Callable<WebDriver>>();
        for (final String browser : browsers) {
            callables.add(new Callable<WebDriver>() {
                public WebDriver call() throws Exception {
                    return createWebDriver(browserURL, null, browser);
                }
            });
        }
        //invoke all the callables, and wait for each thread to return
        try {
            List<Future<WebDriver>> futures = service.invokeAll(callables);
            for (Future<WebDriver> future : futures) {
                webDrivers.add(future.get());
            }
        } catch (InterruptedException e) {
            throw new IllegalArgumentException("Error retrieving webdriver", e);
        } catch (ExecutionException e) {
            throw new IllegalArgumentException("Error retrieving webdriver", e);
        }
        service.shutdown();

        return Collections.unmodifiableList(webDrivers);
    }
}
