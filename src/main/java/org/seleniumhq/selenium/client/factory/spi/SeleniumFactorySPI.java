package org.seleniumhq.selenium.client.factory.spi;

import com.thoughtworks.selenium.Selenium;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;

/**
 * SPI implemented by the {@link Selenium} implementation providers.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SeleniumFactorySPI {
    /**
     *
     * @return
     *      null if the implementation didn't recognize the URI specified in the factory.
     *      returning null causes {@link SeleniumFactory} to try other SPIs found in the system.
     * @throws IllegalArgumentException
     *      If the URI was recognized by the SPI but some of its configurations were wrong,
     *      or if the SPI failed to instantiate the Selenium driver, throw an exception.
     *      {@link SeleniumFactory} will not try other SPIs and propagate the problem to the calling
     *      user application.
     */
    public abstract Selenium createSelenium(SeleniumFactory factory, String browserURL);
}
