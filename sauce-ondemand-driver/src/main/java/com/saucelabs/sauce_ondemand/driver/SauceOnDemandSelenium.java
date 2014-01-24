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
import com.thoughtworks.selenium.Selenium;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Additional methods exposed by {@link Selenium} returned from the "sauce-ondemand:..." URL.
 *
 * @author Kohsuke Kawaguchi
 */
public interface SauceOnDemandSelenium {
    /**
     * Obtains the session ID of this Selenium.
     *
     * <p>
     * If the session has ended via {@link Selenium#stop()},
     * this method returns the session ID of the last session.
     *
     * @return null
     *      if the session hasn't started yet.
     */
    String getSessionIdValue();

    /**
     * Gets the credential used to connect to Sauce OnDemand.
     * This information is useful when retrieving server log and video.
     */
    Credential getCredential();

    /**
     * Obtains the URL for downloading the Selenium server log file.
     * <p>
     * Note that to acccess this URL you need to send in the credential through BASIC auth.
     * Note that this file only becomes available some time after the test is stopped.
     */
    URL getSeleniumServerLogFile() throws IOException;

    /**
     * Obtains the URL for downloading the video recording <tt>video.flv</tt>.
     * <p>
     * Note that to acccess this URL you need to send in the credential through BASIC auth.
     * Note that this file only becomes available some time after the test is stopped.
     */
    URL getVideo() throws IOException;

    /**
     * Retrieves the contents of the Selenium server log file.
     * <p>
     * Note that this file only becomes available some time after the test is stopped.
     */
    InputStream getSeleniumServerLogFileInputStream() throws IOException;

    /**
     * Retrieves the contents of the video recording.
     * <p>
     * Note that this file only becomes available some time after the test is stopped.
     */
    InputStream getVideoInputStream() throws IOException;

    void jobPassed() throws IOException;

    void jobFailed() throws IOException;

    void setBuildNumber(String buildNumber) throws IOException;

    void setJobName(String jobName) throws IOException;
}
