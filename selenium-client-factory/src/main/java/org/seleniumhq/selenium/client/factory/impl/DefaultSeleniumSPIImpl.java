package org.seleniumhq.selenium.client.factory.impl;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

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
