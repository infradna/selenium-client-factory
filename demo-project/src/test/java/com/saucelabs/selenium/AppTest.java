package com.saucelabs.selenium;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;

public class AppTest extends TestCase {
    public void test1() throws InterruptedException {
        Selenium s = SeleniumFactory.create();
        s.start();
        s.open("/");
        System.out.println(s.getTitle());
        Thread.sleep(3000); // manually induced delay, or else the window closes too fast
        s.stop();
    }
}
