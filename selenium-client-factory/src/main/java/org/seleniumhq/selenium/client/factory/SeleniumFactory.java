package org.seleniumhq.selenium.client.factory;

import com.thoughtworks.selenium.Selenium;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 * @author Kohsuke Kawaguchi
 */
public class SeleniumFactory {
    // TODO: browserURL should be also inferred from the system property and env var.
    public static Selenium create(String browserURL) {
        return new SeleniumFactory().createSelenium(browserURL);
    }

    public static Selenium create(String driverUri, String browserURL) {
        return new SeleniumFactory().setUri(driverUri).createSelenium(browserURL);
    }

    private String uri;
    private ClassLoader cl = Thread.currentThread().getContextClassLoader();
    private Map<String,Object> properties = new HashMap<String,Object>();

    public SeleniumFactory() {
        uri = System.getProperty("SELENIUM_DRIVER");
        if (uri==null)
            uri = System.getenv("SELENIUM_DRIVER");
    }

    public String getUri() {
        return uri;
    }

    public SeleniumFactory setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public SeleniumFactory setClassLoader(ClassLoader cl) {
        this.cl = cl;
        return this;
    }

    public SeleniumFactory setProperty(String key, Object value) {
        this.properties.put(key,value);
        return this;
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public Map<String,Object> getProperties() {
        return this.properties;
    }

    /**
     * Creates a clone of this factory that's identically configured.
     */
    public SeleniumFactory clone() {
        SeleniumFactory f = new SeleniumFactory();
        f.uri = uri;
        f.cl = cl;
        f.properties.clear();
        f.properties.putAll(properties);
        return f;
    }

    public Selenium createSelenium(String browserURL) {
        try {
            if (uri==null)
                throw new IllegalArgumentException("Selenium driver URI is not set");

            Enumeration<URL> e = cl.getResources("META-INF/services/" + SeleniumFactorySPI.class.getName());
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
                            if (_spi instanceof SeleniumFactorySPI) {
                                SeleniumFactorySPI spi = (SeleniumFactorySPI) _spi;

                                // if this throws an exception, that's a fatal error and we'll abort
                                Selenium selenium = spi.createSelenium(this, browserURL);
                                if (selenium!=null)
                                    return selenium;
                                // if the SPI returns null, we'll try next one
                            } else {
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

            throw new IllegalArgumentException(String.format(
                    "Unrecognized Selenium driver URI '%s'. Make sure you got the proper driver jars in your classpath, or increase the logging level to get more information.", uri));

        } catch (IOException x) {
            throw new IllegalArgumentException("Failed to instantiate the driver",x);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(SeleniumFactory.class.getName());
}
