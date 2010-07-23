package org.seleniumhq.selenium.client.htmlunit;

import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;

/**
 * @author Kohsuke Kawaguchi
 */
public class HtmlUnitTest extends TestCase {
    public void test1() {
        Selenium s = SeleniumFactory.create("htmlunit:", "http://www.google.com/");
        s.start();
        s.open("http://www.google.com/");
        assertEquals("Google",s.getTitle());
        s.stop();
    }
}
