package org.seleniumhq.selenium.client.logging;

import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;

/**
 * @author Kohsuke Kawaguchi
 */
public class LoggingSeleniumTest extends TestCase {
    public void test1() {
        Selenium s = SeleniumFactory.create("log:embedded-rc:*firefox", "http://www.google.com/");
        assertTrue(s instanceof LoggingSelenium);
        s.start();
        s.open("http://www.google.com/");
        assertEquals("Google",s.getTitle());
        s.stop();
    }
}
