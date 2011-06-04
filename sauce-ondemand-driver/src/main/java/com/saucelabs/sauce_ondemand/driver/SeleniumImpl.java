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
import com.saucelabs.rest.JobFactory;
import com.saucelabs.rest.UpdateJob;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
class SeleniumImpl extends DefaultSelenium implements SauceOnDemandSelenium, Selenium {
    /**
     * {@link DefaultSelenium} throw away the session ID as soon as the {@link #stop()}
     * is called, so we'll  store it aside.
     */
    private String lastSessionId;

    private String jobName;

    private final Credential credential;

    SeleniumImpl(String serverHost, int serverPort, String browserStartCommand, String browserURL, Credential credential, String jobName) {
        super(serverHost, serverPort, browserStartCommand, browserURL);
        this.credential = credential;
        this.jobName = jobName;
    }

    @Override
    public void start() {
        super.start();
        dumpSessionId();
    }

    @Override
    public void start(String optionsString) {
        super.start(optionsString);
        dumpSessionId();
    }

    @Override
    public void start(Object optionsObject) {
        super.start(optionsObject);
        dumpSessionId();
    }

    /**
     * Dump the session ID, so that it can be captured by the CI server.
     */
    private void dumpSessionId() {
        lastSessionId = getSessionId();
        System.out.println("SauceOnDemandSessionID=" + lastSessionId + " job-name=" + jobName);
    }
    
    public String getSessionId() {
        try {
            Field f = commandProcessor.getClass().getDeclaredField("sessionId");
            f.setAccessible(true);
            Object id = f.get(commandProcessor);
            if (id!=null)   return id.toString();
            return lastSessionId;
        } catch (NoSuchFieldException e) {
            // failed to retrieve the session ID
        } catch (IllegalAccessException e) {
            // failed to retrieve the session ID
        }
        return null;
    }

    public String getSessionIdValue() {
        return getSessionId();
    }

    public Credential getCredential() {
        return credential;
    }

    public URL getSeleniumServerLogFile() throws IOException {
        return getFileURL("selenium-server.log");
    }

    public URL getVideo() throws IOException {
        return getFileURL("video.flv");
    }

    private URL getFileURL(String fileName) throws MalformedURLException {
        // userinfo in URL doesn't result in the BASIC auth, so in this method we won't set the credential.
        return new URL(MessageFormat.format("https://saucelabs.com/rest/{0}/jobs/{1}/results/{2}",
                credential.getUsername(), lastSessionId, fileName));
    }

    public InputStream getSeleniumServerLogFileInputStream() throws IOException {
        return openWithAuth(getSeleniumServerLogFile());
    }

    public InputStream getVideoInputStream() throws IOException {
        return openWithAuth(getVideo());
    }

    private InputStream openWithAuth(URL url) throws IOException {
        URLConnection con = url.openConnection();
        String encodedAuthorization = new BASE64Encoder().encode(
                (credential.getUsername() + ":" + credential.getKey()).getBytes());
        con.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        return con.getInputStream();
    }

    public void jobPassed() throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", true);
        updateJobInfo(updates);
    }

    private void updateJobInfo(Map<String,Object> updates) throws IOException {
        JobFactory jobFactory = new JobFactory(credential);
        jobFactory.update(lastSessionId, updates);
    }

    public void jobFailed() throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", true);
        updateJobInfo(updates);
    }

    public void setBuildNumber(String buildNumber) throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", buildNumber);
        updateJobInfo(updates);
    }
}
