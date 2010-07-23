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
