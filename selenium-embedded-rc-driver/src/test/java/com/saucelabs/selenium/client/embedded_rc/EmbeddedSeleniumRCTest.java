package com.saucelabs.selenium.client.embedded_rc;

import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;

/**
 * @author Kohsuke Kawaguchi
 */
public class EmbeddedSeleniumRCTest extends TestCase {
    public void test1() {
        Selenium s = SeleniumFactory.create("embedded-rc:*firefox", "http://www.google.com/");
        s.start();
        s.open("http://www.google.com/");
        assertEquals("Google",s.getTitle());
        s.stop();
    }
}
