package com.saucelabs.selenium;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;

public class AppTest extends TestCase {
    public void test1() {
        Selenium s = SeleniumFactory.create();
        s.start();
        s.open("/");
        System.out.println(s.getTitle());
        s.close();
    }
}
