package com.saucelabs.selenium.client.embedded_rc;

import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.cli.RemoteControlLauncher;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * {@link SeleniumFactorySPI} implementation that lets you run Selenium RC inside the same JVM.
 *
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class EmbeddedRcSPIImpl extends SeleniumFactorySPI {
    private static final String JETTY_FORM_SIZE = "org.openqa.jetty.http.HttpRequest.maxFormContentSize";

    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!uri.startsWith(SCHEME))        return null;    // not ours
        String browser = uri.substring(SCHEME.length());

        // allow the additional parameters to be passed in.
        String[] args = (String[])factory.getProperty("embedded_args");
        if (args==null) args = new String[0];
        RemoteControlConfiguration configuration = RemoteControlLauncher.parseLauncherOptions(args);

        int port = allocateRandomPort();
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

    private static final String SCHEME = "embedded-rc:";
}
