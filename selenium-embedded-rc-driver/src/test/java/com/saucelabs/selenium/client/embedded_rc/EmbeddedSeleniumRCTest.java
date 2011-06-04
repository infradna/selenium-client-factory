/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saucelabs.selenium.client.embedded_rc;

import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import org.openqa.selenium.WebDriver;

/**
 * @author Kohsuke Kawaguchi
 */
public class EmbeddedSeleniumRCTest extends TestCase {
    public void testSelenium() {
        Selenium s = SeleniumFactory.create("embedded-rc:*firefox", "http://www.google.com/");
        s.start();
        s.open("/");
        assertEquals("Google",s.getTitle());
        s.stop();
    }

    public void testWebDriver() {
        WebDriver s = SeleniumFactory.createWebDriver("embedded-rc:*firefox", "http://www.google.com/");        
        assertEquals("Google",s.getTitle());
        s.quit();
    }
}
