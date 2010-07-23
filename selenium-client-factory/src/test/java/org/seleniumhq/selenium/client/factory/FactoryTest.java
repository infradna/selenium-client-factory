package org.seleniumhq.selenium.client.factory;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class FactoryTest extends TestCase {
    public void test1() {
        assertNotNull(SeleniumFactory.create("http://no.such.host:4444/*firefox","http://www.google.com/"));
    }

    public void testFailedInstantiation() {
        try {
            SeleniumFactory.create("bogus:uri","http://www.google.com/");
            fail("Expected to fail");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // success
        }
    }
}
