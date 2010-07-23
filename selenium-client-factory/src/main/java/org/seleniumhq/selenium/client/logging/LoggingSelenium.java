package org.seleniumhq.selenium.client.logging;

import com.thoughtworks.selenium.Selenium;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Selenium} returned from "log:..." URI will implement this interface
 * to let you control its logging behavior.
 *
 * @author Kohsuke Kawaguchi
 */
public interface LoggingSelenium {
    void setLogger(Logger logger);
    Logger getLogger();

    void setLogLevel(Level level);
    Level getLogLevel();

    Selenium getBaseDriver();
    void setBaseDriver(Selenium selenium);

    String getId();
    void setId(String id);
}
