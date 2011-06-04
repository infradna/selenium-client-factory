package com.saucelabs.selenium.client.embedded_rc;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;

/**
 * @author Ross Rowe
 */
public class WebDriverFilter implements WebDriver {
    private WebDriver base;

    public WebDriverFilter(WebDriver base) {
        this.base = base;
    }

    public void get(String s) {
        base.get(s);
    }

    public String getCurrentUrl() {
        return base.getCurrentUrl();
    }

    public String getTitle() {
        return base.getTitle();
    }

    public List<WebElement> findElements(By by) {
        return base.findElements(by);
    }

    public WebElement findElement(By by) {
        return base.findElement(by);
    }

    public String getPageSource() {
        return base.getPageSource();
    }

    public void close() {
        base.close();
    }

    public void quit() {
        base.quit();
    }

    public Set<String> getWindowHandles() {
        return base.getWindowHandles();
    }

    public String getWindowHandle() {
        return base.getWindowHandle();
    }

    public TargetLocator switchTo() {
        return base.switchTo();
    }

    public Navigation navigate() {
        return base.navigate();
    }

    public Options manage() {
        return base.manage();
    }
}
