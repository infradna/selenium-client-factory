package org.seleniumhq.selenium.client.logging;

import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class LoggingSeleniumTest extends TestCase {
    private List<LogRecord> logs = new ArrayList<LogRecord>();

    public void test1() {
        Selenium s = SeleniumFactory.create("log:embedded-rc:*firefox", "http://www.google.com/");

        LoggingSelenium ls = (LoggingSelenium)s;
        Logger l = Logger.getAnonymousLogger();
        ls.setLogger(l);
        l.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        s.start();
        s.open("http://www.google.com/");
        assertEquals("Google",s.getTitle());
        s.stop();

        verifyLog();
    }

    private void verifyLog() {
        for (LogRecord log : logs) {
            if (log.getMessage().contains("open(\"http://www.google.com/\")"))
                return; // found it
        }
        fail("Log not recorded");
    }
}
