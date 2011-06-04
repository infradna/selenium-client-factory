package com.saucelabs.sauce_ondemand.driver;

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.JobFactory;
import com.saucelabs.rest.UpdateJob;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ross Rowe
 */
class RemoteWebDriverImpl extends RemoteWebDriver implements WebDriver, SauceOnDemandSelenium {
    private SessionId lastSessionId;

    private String jobName;

    private final Credential credential;

    RemoteWebDriverImpl(URL url, DesiredCapabilities capabilities, Credential credential, String jobName) {
        super(url, capabilities);
        this.credential = credential;
        this.jobName = jobName;
    }

    @Override
    public void startClient() {
        super.startClient();
        dumpSessionId();
    }

    @Override
    public void get(String url) {
        super.get(url);
        dumpSessionId();
    }

    /**
     * Dump the session ID, so that it can be captured by the CI server.
     */
    private void dumpSessionId() {
        lastSessionId = getSessionId();
        if (lastSessionId == null) {
            System.out.println("SauceOnDemandSessionID=NULL job-name=" + jobName);
        } else {
            System.out.println("SauceOnDemandSessionID=" + lastSessionId.toString() + " job-name=" + jobName);
        }
    }

    public String getSessionIdValue() {
        return getSessionId().toString();
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

   public void jobPassed() throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", true);
        updateJobInfo(updates);
    }

    private void updateJobInfo(Map<String,Object> updates) throws IOException {
        JobFactory jobFactory = new JobFactory(credential);
        jobFactory.update(getSessionIdValue(), updates);
    }

    public void jobFailed() throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", true);
        updateJobInfo(updates);
    }

    private InputStream openWithAuth(URL url) throws IOException {
        URLConnection con = url.openConnection();
        String encodedAuthorization = new BASE64Encoder().encode(
                (credential.getUsername() + ":" + credential.getKey()).getBytes());
        con.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        return con.getInputStream();
    }

    public void setBuildNumber(String buildNumber) throws IOException {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", buildNumber);
        updateJobInfo(updates);
    }

}
