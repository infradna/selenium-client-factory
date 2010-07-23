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

/**
 * {@link Selenium} that delegates method calls to another instance.
 * Useful as a base class.
 * 
 * @author Kohsuke Kawaguchi
 */
public class SeleniumFilter implements Selenium {
    private final Selenium base;

    public SeleniumFilter(Selenium base) {
        this.base = base;
    }

    /**
     * Provides access to the underlying {@link Selenium} instance.
     */
    public Selenium getBase() {
        return base;
    }

    public void setExtensionJs(String extensionJs) {
        base.setExtensionJs(extensionJs);
    }

    public void start() {
        base.start();
    }

    public void start(String optionsString) {
        base.start(optionsString);
    }

    public void start(Object optionsObject) {
        base.start(optionsObject);
    }

    public void stop() {
        base.stop();
    }

    public void showContextualBanner() {
        base.showContextualBanner();
    }

    public void showContextualBanner(String className, String methodName) {
        base.showContextualBanner(className, methodName);
    }

    public void click(String locator) {
        base.click(locator);
    }

    public void doubleClick(String locator) {
        base.doubleClick(locator);
    }

    public void contextMenu(String locator) {
        base.contextMenu(locator);
    }

    public void clickAt(String locator, String coordString) {
        base.clickAt(locator, coordString);
    }

    public void doubleClickAt(String locator, String coordString) {
        base.doubleClickAt(locator, coordString);
    }

    public void contextMenuAt(String locator, String coordString) {
        base.contextMenuAt(locator, coordString);
    }

    public void fireEvent(String locator, String eventName) {
        base.fireEvent(locator, eventName);
    }

    public void focus(String locator) {
        base.focus(locator);
    }

    public void keyPress(String locator, String keySequence) {
        base.keyPress(locator, keySequence);
    }

    public void shiftKeyDown() {
        base.shiftKeyDown();
    }

    public void shiftKeyUp() {
        base.shiftKeyUp();
    }

    public void metaKeyDown() {
        base.metaKeyDown();
    }

    public void metaKeyUp() {
        base.metaKeyUp();
    }

    public void altKeyDown() {
        base.altKeyDown();
    }

    public void altKeyUp() {
        base.altKeyUp();
    }

    public void controlKeyDown() {
        base.controlKeyDown();
    }

    public void controlKeyUp() {
        base.controlKeyUp();
    }

    public void keyDown(String locator, String keySequence) {
        base.keyDown(locator, keySequence);
    }

    public void keyUp(String locator, String keySequence) {
        base.keyUp(locator, keySequence);
    }

    public void mouseOver(String locator) {
        base.mouseOver(locator);
    }

    public void mouseOut(String locator) {
        base.mouseOut(locator);
    }

    public void mouseDown(String locator) {
        base.mouseDown(locator);
    }

    public void mouseDownRight(String locator) {
        base.mouseDownRight(locator);
    }

    public void mouseDownAt(String locator, String coordString) {
        base.mouseDownAt(locator, coordString);
    }

    public void mouseDownRightAt(String locator, String coordString) {
        base.mouseDownRightAt(locator, coordString);
    }

    public void mouseUp(String locator) {
        base.mouseUp(locator);
    }

    public void mouseUpRight(String locator) {
        base.mouseUpRight(locator);
    }

    public void mouseUpAt(String locator, String coordString) {
        base.mouseUpAt(locator, coordString);
    }

    public void mouseUpRightAt(String locator, String coordString) {
        base.mouseUpRightAt(locator, coordString);
    }

    public void mouseMove(String locator) {
        base.mouseMove(locator);
    }

    public void mouseMoveAt(String locator, String coordString) {
        base.mouseMoveAt(locator, coordString);
    }

    public void type(String locator, String value) {
        base.type(locator, value);
    }

    public void typeKeys(String locator, String value) {
        base.typeKeys(locator, value);
    }

    public void setSpeed(String value) {
        base.setSpeed(value);
    }

    public String getSpeed() {
        return base.getSpeed();
    }

    public void check(String locator) {
        base.check(locator);
    }

    public void uncheck(String locator) {
        base.uncheck(locator);
    }

    public void select(String selectLocator, String optionLocator) {
        base.select(selectLocator, optionLocator);
    }

    public void addSelection(String locator, String optionLocator) {
        base.addSelection(locator, optionLocator);
    }

    public void removeSelection(String locator, String optionLocator) {
        base.removeSelection(locator, optionLocator);
    }

    public void removeAllSelections(String locator) {
        base.removeAllSelections(locator);
    }

    public void submit(String formLocator) {
        base.submit(formLocator);
    }

    public void open(String url) {
        base.open(url);
    }

    public void openWindow(String url, String windowID) {
        base.openWindow(url, windowID);
    }

    public void selectWindow(String windowID) {
        base.selectWindow(windowID);
    }

    public void selectPopUp(String windowID) {
        base.selectPopUp(windowID);
    }

    public void deselectPopUp() {
        base.deselectPopUp();
    }

    public void selectFrame(String locator) {
        base.selectFrame(locator);
    }

    public boolean getWhetherThisFrameMatchFrameExpression(String currentFrameString, String target) {
        return base.getWhetherThisFrameMatchFrameExpression(currentFrameString, target);
    }

    public boolean getWhetherThisWindowMatchWindowExpression(String currentWindowString, String target) {
        return base.getWhetherThisWindowMatchWindowExpression(currentWindowString, target);
    }

    public void waitForPopUp(String windowID, String timeout) {
        base.waitForPopUp(windowID, timeout);
    }

    public void chooseCancelOnNextConfirmation() {
        base.chooseCancelOnNextConfirmation();
    }

    public void chooseOkOnNextConfirmation() {
        base.chooseOkOnNextConfirmation();
    }

    public void answerOnNextPrompt(String answer) {
        base.answerOnNextPrompt(answer);
    }

    public void goBack() {
        base.goBack();
    }

    public void refresh() {
        base.refresh();
    }

    public void close() {
        base.close();
    }

    public boolean isAlertPresent() {
        return base.isAlertPresent();
    }

    public boolean isPromptPresent() {
        return base.isPromptPresent();
    }

    public boolean isConfirmationPresent() {
        return base.isConfirmationPresent();
    }

    public String getAlert() {
        return base.getAlert();
    }

    public String getConfirmation() {
        return base.getConfirmation();
    }

    public String getPrompt() {
        return base.getPrompt();
    }

    public String getLocation() {
        return base.getLocation();
    }

    public String getTitle() {
        return base.getTitle();
    }

    public String getBodyText() {
        return base.getBodyText();
    }

    public String getValue(String locator) {
        return base.getValue(locator);
    }

    public String getText(String locator) {
        return base.getText(locator);
    }

    public void highlight(String locator) {
        base.highlight(locator);
    }

    public String getEval(String script) {
        return base.getEval(script);
    }

    public boolean isChecked(String locator) {
        return base.isChecked(locator);
    }

    public String getTable(String tableCellAddress) {
        return base.getTable(tableCellAddress);
    }

    public String[] getSelectedLabels(String selectLocator) {
        return base.getSelectedLabels(selectLocator);
    }

    public String getSelectedLabel(String selectLocator) {
        return base.getSelectedLabel(selectLocator);
    }

    public String[] getSelectedValues(String selectLocator) {
        return base.getSelectedValues(selectLocator);
    }

    public String getSelectedValue(String selectLocator) {
        return base.getSelectedValue(selectLocator);
    }

    public String[] getSelectedIndexes(String selectLocator) {
        return base.getSelectedIndexes(selectLocator);
    }

    public String getSelectedIndex(String selectLocator) {
        return base.getSelectedIndex(selectLocator);
    }

    public String[] getSelectedIds(String selectLocator) {
        return base.getSelectedIds(selectLocator);
    }

    public String getSelectedId(String selectLocator) {
        return base.getSelectedId(selectLocator);
    }

    public boolean isSomethingSelected(String selectLocator) {
        return base.isSomethingSelected(selectLocator);
    }

    public String[] getSelectOptions(String selectLocator) {
        return base.getSelectOptions(selectLocator);
    }

    public String getAttribute(String attributeLocator) {
        return base.getAttribute(attributeLocator);
    }

    public boolean isTextPresent(String pattern) {
        return base.isTextPresent(pattern);
    }

    public boolean isElementPresent(String locator) {
        return base.isElementPresent(locator);
    }

    public boolean isVisible(String locator) {
        return base.isVisible(locator);
    }

    public boolean isEditable(String locator) {
        return base.isEditable(locator);
    }

    public String[] getAllButtons() {
        return base.getAllButtons();
    }

    public String[] getAllLinks() {
        return base.getAllLinks();
    }

    public String[] getAllFields() {
        return base.getAllFields();
    }

    public String[] getAttributeFromAllWindows(String attributeName) {
        return base.getAttributeFromAllWindows(attributeName);
    }

    public void dragdrop(String locator, String movementsString) {
        base.dragdrop(locator, movementsString);
    }

    public void setMouseSpeed(String pixels) {
        base.setMouseSpeed(pixels);
    }

    public Number getMouseSpeed() {
        return base.getMouseSpeed();
    }

    public void dragAndDrop(String locator, String movementsString) {
        base.dragAndDrop(locator, movementsString);
    }

    public void dragAndDropToObject(String locatorOfObjectToBeDragged, String locatorOfDragDestinationObject) {
        base.dragAndDropToObject(locatorOfObjectToBeDragged, locatorOfDragDestinationObject);
    }

    public void windowFocus() {
        base.windowFocus();
    }

    public void windowMaximize() {
        base.windowMaximize();
    }

    public String[] getAllWindowIds() {
        return base.getAllWindowIds();
    }

    public String[] getAllWindowNames() {
        return base.getAllWindowNames();
    }

    public String[] getAllWindowTitles() {
        return base.getAllWindowTitles();
    }

    public String getHtmlSource() {
        return base.getHtmlSource();
    }

    public void setCursorPosition(String locator, String position) {
        base.setCursorPosition(locator, position);
    }

    public Number getElementIndex(String locator) {
        return base.getElementIndex(locator);
    }

    public boolean isOrdered(String locator1, String locator2) {
        return base.isOrdered(locator1, locator2);
    }

    public Number getElementPositionLeft(String locator) {
        return base.getElementPositionLeft(locator);
    }

    public Number getElementPositionTop(String locator) {
        return base.getElementPositionTop(locator);
    }

    public Number getElementWidth(String locator) {
        return base.getElementWidth(locator);
    }

    public Number getElementHeight(String locator) {
        return base.getElementHeight(locator);
    }

    public Number getCursorPosition(String locator) {
        return base.getCursorPosition(locator);
    }

    public String getExpression(String expression) {
        return base.getExpression(expression);
    }

    public Number getXpathCount(String xpath) {
        return base.getXpathCount(xpath);
    }

    public void assignId(String locator, String identifier) {
        base.assignId(locator, identifier);
    }

    public void allowNativeXpath(String allow) {
        base.allowNativeXpath(allow);
    }

    public void ignoreAttributesWithoutValue(String ignore) {
        base.ignoreAttributesWithoutValue(ignore);
    }

    public void waitForCondition(String script, String timeout) {
        base.waitForCondition(script, timeout);
    }

    public void setTimeout(String timeout) {
        base.setTimeout(timeout);
    }

    public void waitForPageToLoad(String timeout) {
        base.waitForPageToLoad(timeout);
    }

    public void waitForFrameToLoad(String frameAddress, String timeout) {
        base.waitForFrameToLoad(frameAddress, timeout);
    }

    public String getCookie() {
        return base.getCookie();
    }

    public String getCookieByName(String name) {
        return base.getCookieByName(name);
    }

    public boolean isCookiePresent(String name) {
        return base.isCookiePresent(name);
    }

    public void createCookie(String nameValuePair, String optionsString) {
        base.createCookie(nameValuePair, optionsString);
    }

    public void deleteCookie(String name, String optionsString) {
        base.deleteCookie(name, optionsString);
    }

    public void deleteAllVisibleCookies() {
        base.deleteAllVisibleCookies();
    }

    public void setBrowserLogLevel(String logLevel) {
        base.setBrowserLogLevel(logLevel);
    }

    public void runScript(String script) {
        base.runScript(script);
    }

    public void addLocationStrategy(String strategyName, String functionDefinition) {
        base.addLocationStrategy(strategyName, functionDefinition);
    }

    public void captureEntirePageScreenshot(String filename, String kwargs) {
        base.captureEntirePageScreenshot(filename, kwargs);
    }

    public void rollup(String rollupName, String kwargs) {
        base.rollup(rollupName, kwargs);
    }

    public void addScript(String scriptContent, String scriptTagId) {
        base.addScript(scriptContent, scriptTagId);
    }

    public void removeScript(String scriptTagId) {
        base.removeScript(scriptTagId);
    }

    public void useXpathLibrary(String libraryName) {
        base.useXpathLibrary(libraryName);
    }

    public void setContext(String context) {
        base.setContext(context);
    }

    public void attachFile(String fieldLocator, String fileLocator) {
        base.attachFile(fieldLocator, fileLocator);
    }

    public void captureScreenshot(String filename) {
        base.captureScreenshot(filename);
    }

    public String captureScreenshotToString() {
        return base.captureScreenshotToString();
    }

    public String captureNetworkTraffic(String type) {
        return base.captureNetworkTraffic(type);
    }

    public void addCustomRequestHeader(String key, String value) {
        base.addCustomRequestHeader(key, value);
    }

    public String captureEntirePageScreenshotToString(String kwargs) {
        return base.captureEntirePageScreenshotToString(kwargs);
    }

    public void shutDownSeleniumServer() {
        base.shutDownSeleniumServer();
    }

    public String retrieveLastRemoteControlLogs() {
        return base.retrieveLastRemoteControlLogs();
    }

    public void keyDownNative(String keycode) {
        base.keyDownNative(keycode);
    }

    public void keyUpNative(String keycode) {
        base.keyUpNative(keycode);
    }

    public void keyPressNative(String keycode) {
        base.keyPressNative(keycode);
    }
}
