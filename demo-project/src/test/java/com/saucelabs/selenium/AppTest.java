package com.saucelabs.selenium;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.openqa.selenium.WebDriver;

public class AppTest extends TestCase {
    public void testSelenium() throws InterruptedException {
        Selenium s = SeleniumFactory.create();
        s.start();
        s.open("/");
        System.out.println(s.getTitle());
        Thread.sleep(3000); // manually induced delay, or else the window closes too fast
        s.stop();
    }

    public void testWebDriver() throws InterruptedException {
        WebDriver s = SeleniumFactory.createWebDriver();

        System.out.println(s.getTitle());
        Thread.sleep(3000); // manually induced delay, or else the window closes too fast
        s.quit();
    }
}
