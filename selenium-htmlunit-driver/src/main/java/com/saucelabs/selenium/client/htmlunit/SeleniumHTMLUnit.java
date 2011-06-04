/*
 * Title: SeleniumHTMLUnit
 *
 * Description:
 * This class implements the selenium API (current API as of 20070801) using htmlunit,
 * in order to have a suitable load-test engine to be driven by TestMaker.
 *
 * This component is part of a package allowing TestMaker 5.x
 * (http://www.pushtotest.com) to execute Selenium-IDE
 * (http://wiki.openqa.org/display/SIDE/Home) recorded test scenarios.
 * This extension package will remain accessible from the Open-Source section
 * of the Denali web site: http://www.denali.be
 *
 */
package com.saucelabs.selenium.client.htmlunit;

/*
 * Main author(s): Olivier Dony, Denali
 * with contributions from other members of the Denali team
 * $Id: SeleniumHTMLUnit.java,v 1.9 2008/06/16 09:02:49 ndaniels Exp $
 */

/*
 * Copyright (c) 2007, Denali Consulting SA, Belgium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Denali Consulting SA nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Denali Consulting SA, Belgium, ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Denali Consulting SA, Belgium BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import com.gargoylesoftware.htmlunit.AjaxController;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.thoughtworks.selenium.Selenium;
import java.util.Date;
import java.util.Map;
import java.util.Stack;

/**
 * An implementation of the Selenium API (current as of 20070801) with HTMLUnit.
 *
 * @author Olivier Dony, Denali s.a.
 * @version $Id: SeleniumHTMLUnit.java,v 1.9 2008/06/16 09:02:49 ndaniels Exp $
 */
public class SeleniumHTMLUnit implements Selenium {

    /** The default timeout for HTTP operations * */
    public static final int DEFAULT_HTTP_TIMEOUT = 60000;
    public static final int DEFAULT_WAIT_TIMEOUT = 60000;

    private static final String CLASS = SeleniumHTMLUnit.class.getName();
    protected WebClient webClient;
    protected static Logger logger = Logger.getLogger(CLASS);
    protected List<Throwable> verificationErrors;
    protected String baseUrl;
    protected boolean debugPageOnFailure;
    protected WebWindow originalWindow;
    private boolean verifyActAsAssert;
    private static boolean printErrorPage=true;
    private Stack<URL> history = new  Stack<URL> ();

    static {
        logger.setLevel(Level.WARNING);
    }
    private  HtmlPage currentPage;
    private HtmlPage oldPage;

    public static void setLoglevel(Level l){
        logger.setLevel(l);
        if (l == l.OFF){
            printErrorPage = false;
        } else {
            printErrorPage = true;
        }
    }

    public SeleniumHTMLUnit() {
        this(false,false);
    }


    public SeleniumHTMLUnit(boolean throwExceptionOnScriptError,
            boolean throwExceptionOnFailingStatusCode) {

        // defaults to severe as to not slow down the tests by default
        // attempt to disable commons-httpclient logging
        try {
        /*    Logger
                    .getLogger(
                    "org.apache.commons.httpclient.HttpMethodDirector")
                    .setLevel(Level.OFF); // java.util.logging
            org.apache.log4j.Logger.getLogger(
                    "org.apache.commons.httpclient.HttpMethodDirector")
                    .setLevel(org.apache.log4j.Level.OFF); // log4j logging/*/
        } catch (Throwable t) {
                        /*
                         * ignored, we're just trying to turn off useless commons-http
                         * logging
                         */
        }
        initWebClient(throwExceptionOnScriptError, throwExceptionOnFailingStatusCode);

    }

    int delay = 0;
    private  void sleep(){
        if (delay == 0)
            return;
        try {
            Thread.sleep(delay);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void initWebClient(boolean throwExceptionOnScriptError,
            boolean throwExceptionOnFailingStatusCode) {
        webClient = new WebClient(BrowserVersion.FIREFOX_2);
        webClient.setAjaxController(new AjaxController(){

        });
        originalWindow = webClient.getCurrentWindow();
        webClient.setThrowExceptionOnScriptError(throwExceptionOnScriptError);
        webClient.setRedirectEnabled(true);
        webClient.setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode);
        webClient.setPrintContentOnFailingStatusCode(printErrorPage);
        verificationErrors = new ArrayList<Throwable>();
        verifyActAsAssert = false;

        // Set a default timeout on HTTP connect and read, as by default
        // WebClient waits forever!
        webClient.setTimeout(DEFAULT_HTTP_TIMEOUT);
        try {
            currentPage = (HtmlPage) webClient.getPage(WebClient.URL_ABOUT_BLANK);
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * Returns the baseUrl. <strong>Important note</strong>: please keep in
     * mind that this 'baseUrl' is not combined with relative URLs in the usual
     * sense, but always concatenated. That means that whenever you
     * {@link #open(String)} a relative URL, the resulting URL will always be
     * baseURL+relativeUrl, no matter what.
     *
     * @return the baseUrl.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    private void setCurrentPage(HtmlPage newPage){

        history.push(currentPage.getWebResponse().getRequestUrl());
        currentPage = newPage;
    }

    /**
     * Sets the baseUrl to use when a URL passed to {@link #open(String)} is not
     * absolute. <strong>Important note</strong>: please keep in mind that this
     * 'baseUrl' is not combined with relative URLs in the usual sense, but
     * always concatenated. That means that whenever you {@link #open(String)} a
     * URL, the resulting URL will always be baseURL+relativeUrl, no matter
     * what. <strong>DO NOT SET A BASEURL UNLESS YOU REALLY MEAN IT</strong>.
     *
     * @param baseUrl
     *            the baseUrl to set.
     */
    public void setBaseUrl(String theBaseUrl) {
        if (theBaseUrl.endsWith("/"))
            baseUrl = theBaseUrl.substring(0,theBaseUrl.length()-1);
        else
            baseUrl = theBaseUrl;
    }

    /**
     * Set the {@link Level} for the {@link Logger} used by this
     * SeleniumHTMLUnit.
     *
     * @param level
     *            a String containing the label of the value for a {@link Level}
     *            (e.g. &quot;INFO&quot; or &quot;800&quot; for
     *            {@link Level#INFO}).
     * @see Level
     */
   /* public void setLogLevel(String level) {
        logger.setLevel(Level.parse(level));
    }*/

    /**
     * Return the label ({@link Level#getName()}) for the {@link Logger} used
     * by this SeleniumHTMLUnit.
     *
     * @see Level
     */
    public String getLogLevel() {
        return logger.getLevel().getName();
    }

    public void setLogLevel(String level) {
        logger.setLevel(Level.parse(level));
    }

    /**
     * Returns whether page debugging is enabled when a failure occurs.
     *
     * @return whether page debugging is enabled when a failure occurs.
     */
    public boolean hasDebugPageOnFailure() {
        return debugPageOnFailure;
    }

    /**
     * Sets whether page debugging is enabled when a failure occurs.
     *
     * @param debugPageOnFailure
     *            true if debugging should be enabled.
     */
    public void setDebugPageOnFailure(boolean debugPageOnFailure) {
        this.debugPageOnFailure = debugPageOnFailure;
    }

    /**
     * Returns the current page, resulting from the last action, and on which
     * the next action will be executed. The current page can also be changed
     * indirectly using {@link #selectWindow(String)}.
     *
     * @return the current page, resulting from the last action, and on which
     *         the next action will be executed.
     * @see #selectWindow(String)
     */
    public HtmlPage getPage() {
        return currentPage;
    }

    /**
     * Returns the underlying htmlunit webClient.
     *
     * @return the underlying htmlunit webClient.
     */
    public WebClient getWebClient() {
        return webClient;
    }

    public void assertEval(String script,String pattern){

        assertEquals(pattern,getEval(script));
    }

    /**
     * Opens an URL in the test frame. This accepts both absolute URLs and URLs
     * relative to the {@link #getBaseUrl()}. <strong>Important:</strong>
     * Relative URLs are not combined with the baseURL, but simply concatenated
     * with it.
     *
     * The "open" command waits for the page to load before proceeding, ie. the
     * "AndWait" suffix is implicit. <em>Note</em>: The URL must be on the
     * same domain as the runner HTML due to security restrictions in the
     * browser (Same Origin Policy). If you need to open an URL on another
     * domain, use the Selenium Server to start a new browser session on that
     * domain.
     *
     * @param location
     *            the URL to open; may be relative or absolute
     */
    public void open(String location) {

        if (!location.startsWith("/")){
            try {
                new URL(location);
            } catch (Exception e){
                location = "/"+location;
            }
        }
        // Note: Selenium has a peculiar notion of relative URLs: the relative
        // URL
        // is appended directly to the base URL, no matter what this
        // relative URL is (even if it starts with a forward slash (/) )
        try {
            URL url = (baseUrl != null) ? new URL(baseUrl + location)
            : new URL(location);
            setCurrentPage((HtmlPage)webClient.getPage(url));
            logger.info("open: opened url = " + location);
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "open: URL is malformed, maybe the baseURL is incorrect: ",
                    e);
        } catch (IOException e) {
            throw new RuntimeException("open: failed to open URL: ", e);
        }
        sleep();
    }

    /**
     * Sets the value of an input field, as though you typed it in.
     * <p>
     * Can also be used to set the value of combo boxes, check boxes, etc. In
     * these cases, value should be the value of the option selected, not the
     * visible text.
     * </p>
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     * @param value
     *            the value to type
     */
    public void type(String locator, String value) {
        List<? extends Object> results = getElements(locator);
        for (Object elemObj : results) {
            HtmlElement elem = (HtmlElement) elemObj;
            try {
                elem.focus();
                if(elem instanceof HtmlFileInput)//Hack because type method is not working on upload input
                    ((HtmlFileInput)elem).setValueAttribute(value);
                else
                    elem.type(value);
            } catch (IOException e) {
                throw new RuntimeException("type: failed to type on element "+locator, e);
            }
            logger.info("type: "+value+" typed in element " + elem.getNodeName());
        }
        sleep();
    }

    /**
     * Clicks on a link, button, checkbox or radio button. If the click action
     * causes a new page to load (like a link usually does), call
     * waitForPageToLoad.
     *
     * @param locator
     *            an element locator
     */
    public void clickAndWait(String locator){
        click(locator);
    }

    /**
     * Clicks on a link, button, checkbox or radio button. If the click action
     * causes a new page to load (like a link usually does), call
     * waitForPageToLoad.
     *
     * @param locator
     *            an element locator
     */
    public void click(String locator) {
        try {
            HtmlElement elem = getElement(locator);

            logger.info("click on " + elem);
            setCurrentPage( (HtmlPage)(elem).click());
        } catch (IOException e) {
            throw new RuntimeException(
                    "click: failed to perform click on element @ locator "
                    + locator, e);
        }
        sleep();
    }

    /**
     * Returns the title of the current page, or null if there is no title.
     *
     * @return the title of the current page, or null if there is no title.
     */
    public String getTitle() {
        try {
            return getPage().getTitleText();
        } catch (Exception e) {
            logger
                    .log(
                    Level.WARNING,
                    "getTitle: could not get page title, maybe there was no page title or head section : "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies that the specified text pattern appears somewhere on the
     * rendered page shown to the user.
     *
     * @param pattern
     *            a <a href="#patterns">pattern</a> to match with the text of
     *            the page
     * @return true if the pattern matches the text, false otherwise
     */
    public boolean isTextPresent(String pattern) {
        return Pattern.compile(pattern).matcher(getPage().asText()).find();
    }

    /**
     * Waits for a new page to load.
     *
     * <p>
     * You can use this command instead of the "AndWait" suffixes,
     * "clickAndWait", "selectAndWait", "typeAndWait" etc. (which are only
     * available in the JS API).
     * </p>
     * <p>
     * Selenium constantly keeps track of new pages loading, and sets a
     * "newPageLoaded" flag when it first notices a page load. Running any other
     * Selenium command after turns the flag to false. Hence, if you want to
     * wait for a page to load, you must wait immediately after a Selenium
     * command that caused a page-load.
     * </p>
     *
     * @param strTimeout
     *            a timeout in milliseconds, after which this command will
     *            return with an error
     */
    public void waitForPageToLoad(String strTimeout) {
        // Selenium is asynchronous but HtmlUnit is synchronous, so this cannot
        // be mapped easily.
        logger.info("Action ignored: waitForPageToLoad, timeout = "
                + strTimeout);
    }

    /**
     * Select an option from a drop-down using an option locator.
     * <p>
     * Option locators provide different ways of specifying options of an HTML
     * Select element (e.g. for selecting a specific option, or for asserting
     * that the selected option satisfies a specification). There are several
     * forms of Select Option Locator.
     * </p>
     * <ul>
     * <li><strong>label</strong>=<em>labelPattern</em>: matches options
     * based on their labels, i.e. the visible text. (This is the default.)
     * <ul class="first last simple">
     * <li>label=regexp:^[Oo]ther</li>
     * </ul>
     * </li>
     * <li><strong>value</strong>=<em>valuePattern</em>: matches options
     * based on their values.
     * <ul class="first last simple">
     * <li>value=other</li>
     * </ul>
     * </li>
     * <li><strong>id</strong>=<em>id</em>: matches options based on their
     * ids.
     * <ul class="first last simple">
     * <li>id=option1</li>
     * </ul>
     * </li>
     * <li><strong>index</strong>=<em>index</em>: matches an option based
     * on its index (offset from zero).
     * <ul class="first last simple">
     * <li>index=2</li>
     * </ul>
     * </li>
     * </ul>
     * <p>
     * If no option locator prefix is provided, the default behaviour is to
     * match on <strong>label</strong>.
     * </p>
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @param optionLocator
     *            an option locator (a label by default)
     */
    public void select(final String selectLocator, final String optionLocator) {
        setSelectedOption(selectLocator, optionLocator, true);
    }

    /**
     * Add a selection to the set of selected options in a multi-select element
     * using an option locator.
     *
     * @see #select for details of option locators
     * @param locator
     *            an <a href="#locators">element locator</a> identifying a
     *            multi-select box
     * @param optionLocator
     *            an option locator (a label by default)
     */
    public void addSelection(String locator, String optionLocator) {
        setSelectedOption(locator, optionLocator, true);
    }

    /**
     * Remove a selection from the set of selected options in a multi-select
     * element using an option locator.
     *
     * @see #select for details of option locators
     * @param locator
     *            an <a href="#locators">element locator</a> identifying a
     *            multi-select box
     * @param optionLocator
     *            an option locator (a label by default)
     */
    public void removeSelection(String locator, String optionLocator) {
        setSelectedOption(locator, optionLocator, false);
    }

    /**
     * Unselects all of the selected options in a multi-select element.
     *
     * @param locator
     *            an <a href="#locators">element locator</a> identifying a
     *            multi-select box
     */
    public void removeAllSelections(String locator) {
        HtmlSelect select = (HtmlSelect) getElement(locator);
        List<HtmlOption> options = select.getOptions();
        for (HtmlOption tmp : options) {
            HtmlPage newPage = (HtmlPage) select.setSelectedAttribute(tmp, false);
            if (newPage != null)
                    setCurrentPage( newPage);
        }
        sleep();
    }

    /**
     * Gets all option labels (visible text) for selected options in the
     * specified select or multi-select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return an array of all selected option labels in the specified select
     *         drop-down
     */
    public String[] getSelectedLabels(String selectLocator) {
        String[] result = null;
        List<HtmlOption> selectedOptions = getSelectedOptions(selectLocator);
        result = new String[selectedOptions.size()];
        int i = 0;
        for (HtmlOption optionItem : selectedOptions) {
            result[i++] = optionItem.asText();
        }
        return result;
    }

    /**
     * Gets option label (visible text) for selected option in the specified
     * select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return the selected option label in the specified select drop-down
     */
    public String getSelectedLabel(String selectLocator) {
        String[] result = getSelectedLabels(selectLocator);
        if (result != null && result.length > 0) {
            return result[0];
        }
        return null;
    }

    /**
     * Gets all option values (value attributes) for selected options in the
     * specified select or multi-select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return an array of all selected option values in the specified select
     *         drop-down
     */
    public String[] getSelectedValues(String selectLocator) {
        String[] result = null;
        List<HtmlOption> selectedOptions = getSelectedOptions(selectLocator);
        result = new String[selectedOptions.size()];
        int i = 0;
        for (HtmlOption optionItem : selectedOptions) {
            result[i++] = optionItem.getValueAttribute();
        }
        return result;
    }

    /**
     * Gets option value (value attribute) for selected option in the specified
     * select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return the selected option value in the specified select drop-down
     */
    public String getSelectedValue(String selectLocator) {
        String[] result = getSelectedValues(selectLocator);
        if (result.length > 0) {
            return result[0];
        }
        return null;
    }

    /**
     * Gets all option indexes (option number, starting at 0) for selected
     * options in the specified select or multi-select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return an array of all selected option indexes in the specified select
     *         drop-down
     */
    public String[] getSelectedIndexes(String selectLocator) {
        String[] result = null;
        HtmlSelect select = (HtmlSelect) getElement(selectLocator);
        List<HtmlOption> options = select.getOptions();
        List<String> list = new ArrayList<String>();
        int i = 0;
        for (HtmlOption option : options) {
            if (option.isSelected()) {
                list.add(String.valueOf(i));
            }
            i++;
        }
        result = new String[list.size()];
        result = list.toArray(result);
        return result;
    }

    /**
     * Gets option index (option number, starting at 0) for selected option in
     * the specified select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return the selected option index in the specified select drop-down
     */
    public String getSelectedIndex(String selectLocator) {
        String[] result = getSelectedIndexes(selectLocator);
        if (result.length > 0) {
            return result[0];
        }
        return "-1";
    }

    /**
     * Gets all option element IDs for selected options in the specified select
     * or multi-select element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return an array of all selected option IDs in the specified select
     *         drop-down
     */
    public String[] getSelectedIds(String selectLocator) {
        String[] result = null;
        List<HtmlOption> selectedOptions = getSelectedOptions(selectLocator);
        result = new String[selectedOptions.size()];
        int i = 0;
        for (HtmlOption optionItem : selectedOptions) {
            result[i++] = optionItem.getId();
        }
        return result;
    }

    /**
     * Gets option element ID for selected option in the specified select
     * element.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return the selected option ID in the specified select drop-down
     */
    public String getSelectedId(String selectLocator) {
        String[] result = getSelectedIds(selectLocator);
        if (result.length > 0) {
            return result[0];
        }
        return null;
    }

    /**
     * Determines whether some option in a drop-down menu is selected.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return true if some option has been selected, false otherwise
     */
    public boolean isSomethingSelected(String selectLocator) {
        HtmlSelect select = (HtmlSelect) getElement(selectLocator);
        return select.getSelectedOptions().size() > 0;
    }

    /**
     * Gets all option labels in the specified select drop-down.
     *
     * @param selectLocator
     *            an <a href="#locators">element locator</a> identifying a
     *            drop-down menu
     * @return an array of all option labels in the specified select drop-down
     */
    public String[] getSelectOptions(String selectLocator) {
        String[] result = null;
        HtmlSelect select = (HtmlSelect) getElement(selectLocator);
        List<HtmlOption> options = select.getOptions();
        result = new String[options.size()];
        int i = 0;
        for (HtmlOption option : options) {
            result[i++] = option.asText();
        }
        return result;
    }

    /**
     * Check a toggle-button (checkbox/radio)
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void check(String locator) {
        HtmlCheckBoxInput checkbox = (HtmlCheckBoxInput) getElement(locator);
        setCurrentPage((HtmlPage)checkbox.setChecked(true));
        sleep();
    }

    /**
     * Uncheck a toggle-button (checkbox/radio)
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void uncheck(String locator) {
        HtmlCheckBoxInput checkbox = (HtmlCheckBoxInput) getElement(locator);
        setCurrentPage ( (HtmlPage) checkbox.setChecked(false));
        sleep();
    }

    /**
     * Gets whether a toggle-button (checkbox/radio) is checked. Fails if the
     * specified element doesn't exist or isn't a toggle-button.
     *
     * @param locator
     *            an <a href="#locators">element locator</a> pointing to a
     *            checkbox or radio button
     * @return true if the checkbox is checked, false otherwise
     */
    public boolean isChecked(String locator) {
        HtmlCheckBoxInput checkbox = (HtmlCheckBoxInput) getElement(locator);
        return checkbox.isChecked();
    }

    /**
     * Simulates a user pressing the mouse button (without releasing it yet) on
     * the specified element.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void mouseDown(String locator) {
        HtmlElement element = getElement(locator);
        setCurrentPage ((HtmlPage) element.mouseDown());
        sleep();
    }

    public void mouseDownRight(String locator) {
        HtmlElement element = getElement(locator);
        setCurrentPage ((HtmlPage) element.mouseDown(false,false,false,1));
        sleep();
    }



    /**
     * Simulates a user moving the mouse pointer away from the specified
     * element.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void mouseOut(String locator) {
        HtmlElement element = getElement(locator);
        setCurrentPage ((HtmlPage) element.mouseOut());
        sleep();
    }

    /**
     * Simulates a user hovering a mouse over the specified element.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void mouseOver(String locator) {
        HtmlElement element = getElement(locator);
        setCurrentPage ( (HtmlPage) element.mouseOver());
        sleep();
    }

    /**
     * Simulates the event that occurs when the user releases the mouse button
     * (i.e., stops holding the button down) on the specified element.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */
    public void mouseUp(String locator) {
        HtmlElement element = getElement(locator);
        setCurrentPage ((HtmlPage) element.mouseUp());
        sleep();
    }

    /**
     * Wait for an element to appear on the current page
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     */

    public void waitForElementPresent(String locator) {
        logger.info("waitForElementPresent "+locator);
        waitForElementPresent(locator, DEFAULT_WAIT_TIMEOUT);

    }

    /**
     * Wait for an element to appear on the current page
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     * @param timeout
     *            the number of millis to wait before timeout
     * @throws InterruptedException
     */

    private void waitForElementPresent(String locator, long timeout) {
        int count = 0;

        for (; count < timeout; count += 500) {
            if (isElementPresent(locator)) {
                break;
            }
            synchronized (getPage()) {
                try {
                    getPage().wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (count >= timeout) {
            System.err.println("Timeout value reached");

        }

    }

    /**
     * Runs the specified JavaScript snippet repeatedly until it evaluates to
     * "true". The snippet may have multiple lines, but only the result of the
     * last line will be considered.
     * <p>
     * Note that, by default, the snippet will be run in the runner's test
     * window, not in the window of your application. To get the window of your
     * application, you can use the JavaScript snippet
     * <code>selenium.browserbot.getCurrentWindow()</code>, and then run your
     * JavaScript in there
     * </p>
     *
     * @param script
     *            the JavaScript snippet to run
     * @param timeout
     *            a timeout in milliseconds, after which this command will
     *            return with an error
     */
    public void waitForCondition(String script, String timeout)

    {


        long theTimeout = Integer.parseInt(timeout);
        long initialTime = new Date().getTime();
        while (true){

            if (new Date().getTime()-initialTime > theTimeout)
                break;

            Object obj = webClient.getJavaScriptEngine().execute(this.getPage(),script,"title",0);


            try {
                Thread.sleep(100);
                Thread.yield();
            } catch (InterruptedException e) {
                throw new java.lang.RuntimeException ("Thread interrupted",e);
            }

            if (obj == null)
                continue;
            if (obj instanceof org.mozilla.javascript.Undefined)
                continue;
            if (obj instanceof Boolean)
                if (((Boolean)obj).booleanValue() == false)
                    continue;
            //not boolean but also not null
           // Thread.sleep(1000);
            //Sleep some time after aceptance is done
            return;

        }
        throw new java.lang.RuntimeException ("waitForCondition: Timeout reached");


        //logger.warning("waitForCondition: action not implemented");
    }

    /**
     * Gets the text of an element. This works for any element that contains
     * text. This command uses either the textContent (Mozilla-like browsers) or
     * the innerText (IE-like browsers) of the element, which is the rendered
     * text shown to the user.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     * @return the text of the element
     */
    public String getText(String locator) {
        HtmlElement elem = getElement(locator);
        return elem.asText();
    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getHtmlSource()
         */
    public String getHtmlSource() {
        return getPage().asXml();
    }

    /**
     * Verifies that the specified element is somewhere on the getPage().
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(String locator) {
        try {
            return getElement(locator) != null;
        } catch (Exception e) {
                        /*
                         * ignored: this is one case where it is legal to fail to locate an
                         * element
                         */
        }
        return false;
    }

    /**
     * Selects a popup window; once a popup window has been selected, all
     * commands go to that window. To select the main window again, use null as
     * the target.
     * <p>
     * Note that there is a big difference between a window's internal
     * JavaScript "name" property and the "title" of a given window's document
     * (which is normally what you actually see, as an end user, in the title
     * bar of the window). The "name" is normally invisible to the end-user;
     * it's the second parameter "windowName" passed to the JavaScript method
     * window.open(url, windowName, windowFeatures, replaceFlag) (which selenium
     * intercepts).
     * </p>
     * <p>
     * Selenium has several strategies for finding the window object referred to
     * by the "windowID" parameter.
     * </p>
     * <p>
     * 1.) if windowID is null, (or the string "null") then it is assumed the
     * user is referring to the original window instantiated by the browser).
     * </p>
     * <p>
     * 2.) if the value of the "windowID" parameter is a JavaScript variable
     * name in the current application window, then it is assumed that this
     * variable contains the return value from a call to the JavaScript
     * window.open() method.
     * </p>
     * <p>
     * 3.) Otherwise, selenium looks in a hash it maintains that maps string
     * names to window "names".
     * </p>
     * <p>
     * 4.) If <i>that</i> fails, we'll try looping over all of the known
     * windows to try to find the appropriate "title". Since "title" is not
     * necessarily unique, this may have unexpected behavior.
     * </p>
     * <p>
     * If you're having trouble figuring out what is the name of a window that
     * you want to manipulate, look at the selenium log messages which identify
     * the names of windows created via window.open (and therefore intercepted
     * by selenium). You will see messages like the following for each window as
     * it is opened:
     * </p>
     * <p>
     * <code>debug: window.open call intercepted; window ID (which you can use with selectWindow()) is "myNewWindow"</code>
     * </p>
     * <p>
     * In some cases, Selenium will be unable to intercept a call to window.open
     * (if the call occurs during or before the "onLoad" event, for example).
     * (This is bug SEL-339.) In those cases, you can force Selenium to notice
     * the open window's name by using the Selenium openWindow command, using an
     * empty (blank) url, like this: openWindow("", "myFunnyWindow").
     * </p>
     *
     * @param windowID
     *            the JavaScript window ID of the window to select
     */
    public void selectWindow(final String windowID) {
        // Selenium's "null" windowID designates the original browser window.
        if (windowID == null || "null".equals(windowID)) {
            webClient.setCurrentWindow(originalWindow);
        } else {
            // throws WebWindowNotFoundException if not found.
            webClient.setCurrentWindow(webClient
                    .getWebWindowByName(windowID != null ? windowID : ""));
        }
        logger.info("selectWindow(" + windowID + "): action done");
    }

    /**
     * Selects a frame within the current window. (You may invoke this command
     * multiple times to select nested frames.) To select the parent frame, use
     * "relative=parent" as a locator; to select the top frame, use
     * "relative=top".
     * <p>
     * You may also use a DOM expression to identify the frame you want
     * directly, like this: <code>dom=frames["main"].frames["subframe"]</code>
     * </p>
     *
     * @param locator
     *            an <a href="#locators">element locator</a> identifying a
     *            frame or iframe
     */
    public void selectFrame(final String frameID) {
        WebWindow frame = null;

        // handle selenium's 'special' frameIDs
        if (frameID != null) {
            if (frameID.startsWith("relative=")) {
                String targetFrame = frameID.substring(9);
                if ("parent".equals(targetFrame) || "up".equals(targetFrame)) {
                    if (webClient.getCurrentWindow() instanceof FrameWindow) {
                        frame = webClient.getCurrentWindow().getParentWindow();
                    } else {
                        // ignored, top frame already.
                        logger
                                .info("selectFrame("
                                + frameID
                                + ") seems to reference current frame, using self.");
                        frame = webClient.getCurrentWindow();
                    }
                } else if ("top".equals(targetFrame)) {
                    frame = webClient.getCurrentWindow().getTopWindow();
                }
            } else if (frameID.startsWith("dom=")) {
                logger
                        .warning("selectFrame() with 'dom=' locator not implemented yet!");
                throw new ElementNotFoundException("frame", "locator", frameID);
            }
        }

        if (frame == null) {
            // No special selenium frame found, locate by id.
            try {
                // throws WebWindowNotFoundException if not found.
                frame = getPage().getFrameByName(frameID != null ? frameID : "");
            } catch (ElementNotFoundException e) {
                // selenium uses frameID="null" for the default frame, so we
                // attempt to
                // find a frame without name
                if (frameID == null || "null".equals(frameID)) {
                    List<FrameWindow> frames = getPage().getFrames();
                    for (Iterator<FrameWindow> iter = frames.iterator(); iter
                            .hasNext();) {
                        FrameWindow frameElem = (FrameWindow) iter.next();
                        if (frameElem.getName() == null) {
                            frame = frameElem;
                            break;
                        }
                    }
                }
                if (frame == null)
                    throw e; // rethrow -- still not found
            }
        }

        webClient.setCurrentWindow(frame);
        logger.info("selectFrame(" + frameID + "): action done");
    }

    /**
     * Gets the (whitespace-trimmed) value of an input field (or anything else
     * with a value parameter). For checkbox/radio elements, the value will be
     * "on" or "off" depending on whether the element is checked or not.
     *
     * @param locator
     *            an <a href="#locators">element locator</a>
     * @return the element value, or "on/off" for checkbox/radio elements
     */
    public String getValue(String locator) {
        HtmlElement elem = getElement(locator);

        if (elem instanceof HtmlRadioButtonInput) {
            HtmlRadioButtonInput input = (HtmlRadioButtonInput) elem;
            return input.isChecked() ? "on" : "off";
        }
        if (elem instanceof HtmlCheckBoxInput) {
            HtmlCheckBoxInput input = (HtmlCheckBoxInput) elem;
            return input.isChecked() ? "on" : "off";
        }
        if (elem instanceof HtmlInput) {
            HtmlInput input = (HtmlInput) elem;
            return input.getValueAttribute();
        }
        throw new RuntimeException(
                "Cannot return value for non-input element with locator :"
                + locator);
    }
    public void stop(){
        List<WebWindow> windows = webClient.getWebWindows();
        for (WebWindow window: windows){
            try {
                window.getEnclosedPage().cleanUp();
                window.setEnclosedPage(null);
            } catch (java.lang.Exception e){
                //Stop browser threads
            }
        }
    }

    // **** Util selenium-rc equivalent methods **** //

    /** Like assertTrue, but fails at the end of the test (during tearDown) */
    public void verifyTrue(boolean b) {
        try {
            Assert.assertTrue(b);
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Like assertTrue, but fails at the end of the test (during tearDown) */
    public void verifyTrue(boolean b, String message) {
        try {
            Assert.assertTrue(message, b);
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Like assertFalse, but fails at the end of the test (during tearDown) */
    public void verifyFalse(boolean b) {
        try {
            Assert.assertFalse(b);
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /**
     * Like assertTextPresent, but fails at the end of the test (during
     * tearDown)
     */
    public void verifyTextPresent(String text) {
        try {
            verifyTrue(isTextPresent(text), "Text \"" + text + "\" not found");
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Check if the text is present in the current page */
    public void assertTextPresent(String text) {
        Assert.assertTrue("Text \"" + text + "\" not found",
                isTextPresent(text));
        logger.info("Text "+text+" found");
    }

    /** Check the title of the current page */
    public void assertTitle(String text) {
        String title = getTitle();
        Assert.assertTrue("Title is not correct, expecting a title matching \""
                + text + "\" and found \"" + title + "\"", title.matches(text));
    }

    /** Check if the text is NOT present in the current page */
    public void assertTextNotPresent(String text) {
        Assert.assertFalse("Text \"" + text + "\" found", isTextPresent(text));
    }

    public void assertTrue(boolean b) {
        Assert.assertTrue(b);
    }

    public void assertFalse(boolean b) {
        Assert.assertFalse(b);
    }

    public void assertTrue(String msg, boolean b) {
        Assert.assertTrue(msg, b);
    }

    public void assertFalse(String msg, boolean b) {
        Assert.assertFalse(msg, b);
    }

    /** Returns the body text of the current page */
        /*
         * public String getText() { return
         * webClient.getJavaScriptEngine().execute(page,
         * "this.page().bodyText()","getText").toString(); }
         */

    /** Like assertEquals, but fails at the end of the test (during tearDown) */
    public void verifyEquals(Object s1, Object s2) {
        try {
            assertEquals(s1, s2);
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Like assertEquals, but fails at the end of the test (during tearDown) */
    public void verifyEquals(boolean s1, boolean s2) {
        try {
            assertEquals(new Boolean(s1), new Boolean(s2));
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Like JUnit's Assert.assertEquals, but knows how to compare string arrays */
    public void assertEquals(Object s1, Object s2) {
        if (s1 instanceof String && s2 instanceof String) {
            assertEquals((String) s1, (String) s2);
        } else if (s1 instanceof String && s2 instanceof String[]) {
            assertEquals((String) s1, (String[]) s2);
        } else if (s1 instanceof String && s2 instanceof Number) {
            assertEquals((String) s1, ((Number) s2).toString());
        } else {
            if (s1 instanceof String[] && s2 instanceof String[]) {

                String[] sa1 = (String[]) s1;
                String[] sa2 = (String[]) s2;
                if (sa1.length != sa2.length) {
                    throw new AssertionFailedError("Expected " + sa1
                            + " but saw " + sa2);
                }
                for (int j = 0; j < sa1.length; j++) {
                    Assert.assertEquals(sa1[j], sa2[j]);
                }
            }
        }
    }

    /**
     * Like JUnit's Assert.assertEquals, but handles "regexp:" strings like HTML
     * Selenese
     */
    public void assertEquals(String s1, String s2) {
        Assert.assertTrue("Expected \"" + s1 + "\" but saw \"" + s2
                + "\" instead", seleniumEquals(s1, s2));
    }

    /**
     * Like JUnit's Assert.assertEquals, but joins the string array with commas,
     * and handles "regexp:" strings like HTML Selenese
     */
    public void assertEquals(String s1, String[] s2) {
        Assert.assertEquals(s1, stringArrayToSimpleString(s2));
    }

    /**
     * Compares two strings, but handles "regexp:" strings like HTML Selenese
     *
     * @param expectedPattern
     * @param actual
     * @return true if actual matches the expectedPattern, or false otherwise
     */
    public boolean seleniumEquals(String expectedPattern, String actual) {
        if (actual.startsWith("regexp:") || actual.startsWith("regex:")) {
            // swap 'em
            String tmp = actual;
            actual = expectedPattern;
            expectedPattern = tmp;
        }
        if (expectedPattern.startsWith("regexp:")) {
            String expectedRegEx = expectedPattern
                    .replaceFirst("regexp:", ".*")
                    + ".*";
            if (!actual.matches(expectedRegEx)) {
                System.out.println("expected " + actual + " to match regexp "
                        + expectedPattern);
                return false;
            }
            return true;
        }
        if (expectedPattern.startsWith("regex:")) {
            String expectedRegEx = expectedPattern.replaceFirst("regex:", ".*")
            + ".*";
            if (!actual.matches(expectedRegEx)) {
                System.out.println("expected " + actual + " to match regex "
                        + expectedPattern);
                return false;
            }
            return true;
        }

        if (expectedPattern.startsWith("exact:")) {
            String expectedExact = expectedPattern.replaceFirst("exact:", "");
            if (!expectedExact.equals(actual)) {
                System.out.println("expected " + actual + " to match "
                        + expectedPattern);
                return false;
            }
            return true;
        }

        String expectedGlob = expectedPattern.replaceFirst("glob:", "");
        expectedGlob = expectedGlob.replaceAll(
                "([\\]\\[\\\\{\\}$\\(\\)\\|\\^\\+.])", "\\\\$1");

        expectedGlob = expectedGlob.replaceAll("\\*", ".*");
        expectedGlob = expectedGlob.replaceAll("\\?", ".");
        if (!Pattern.compile(expectedGlob, Pattern.DOTALL).matcher(actual)
        .matches()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("seleniumEquals: expected \"" + actual
                        + "\" to match glob \"" + expectedPattern
                        + "\" (had transformed the glob into regexp \""
                        + expectedGlob + "\")");
            }
            return false;
        }
        return true;
    }

    /**
     * Compares two objects, but handles "regexp:" strings like HTML Selenese
     *
     * @see #seleniumEquals(String, String)
     * @return true if actual matches the expectedPattern, or false otherwise
     */
    public boolean seleniumEquals(Object expected, Object actual) {
        if (expected instanceof String && actual instanceof String) {
            return seleniumEquals((String) expected, (String) actual);
        }
        return expected.equals(actual);
    }

    /** Asserts that two string arrays have identical string contents */
    public void assertEquals(String[] s1, String[] s2) {
        String comparisonDumpIfNotEqual = verifyEqualsAndReturnComparisonDumpIfNot(
                s1, s2);
        if (comparisonDumpIfNotEqual != null) {
            throw new AssertionFailedError(comparisonDumpIfNotEqual);
        }
    }

    /**
     * Asserts that two string arrays have identical string contents (fails at
     * the end of the test, during tearDown)
     */
    public void verifyEquals(String[] s1, String[] s2) {
        String comparisonDumpIfNotEqual = verifyEqualsAndReturnComparisonDumpIfNot(
                s1, s2);
        if (comparisonDumpIfNotEqual != null) {
            if (verifyActAsAssert) {
                throw new AssertionFailedError(comparisonDumpIfNotEqual);
            } else {
                logger.warning("Verify failed : " + comparisonDumpIfNotEqual);
                addToVerificationErrors(new AssertionFailedError(
                        "Verify failed : " + comparisonDumpIfNotEqual));
            }
        }
    }

    private String verifyEqualsAndReturnComparisonDumpIfNot(String[] s1,
            String[] s2) {
        boolean misMatch = false;
        if (s1.length != s2.length) {
            misMatch = true;
        }
        for (int j = 0; j < s1.length; j++) {
            if (!seleniumEquals(s1[j], s2[j])) {
                misMatch = true;
                break;
            }
        }
        if (misMatch) {
            return "Expected " + stringArrayToString(s1) + " but saw "
                    + stringArrayToString(s2);
        }
        return null;
    }

    private String stringArrayToString(String[] sa) {
        StringBuffer sb = new StringBuffer("{");
        for (int j = 0; j < sa.length; j++) {
            sb.append(" ").append("\"").append(sa[j]).append("\"");
        }
        sb.append(" }");
        return sb.toString();
    }

    private String stringArrayToSimpleString(String[] sa) {
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < sa.length; j++) {
            sb.append(sa[j]);
            if (j < sa.length - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /** Like assertNotEquals, but fails at the end of the test (during tearDown) */
    public void verifyNotEquals(Object s1, Object s2) {
        try {
            assertNotEquals(s1, s2);
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {
                logger.warning("Verify failed : " + s1.toString()
                + " is equal to " + s2.toString());
                addToVerificationErrors(e);
            }
        }
    }

    /** Like assertNotEquals, but fails at the end of the test (during tearDown) */
    public void verifyNotEquals(boolean s1, boolean s2) {
        try {
            assertNotEquals(new Boolean(s1), new Boolean(s2));
        } catch (AssertionFailedError e) {
            if (verifyActAsAssert) {
                throw e;
            } else {

                logger.warning("Verify failed : " + e.getMessage());
                addToVerificationErrors(e);
            }
        }
    }

    /** Asserts that two objects are not the same (compares using .equals()) */
    public void assertNotEquals(Object obj1, Object obj2) {
        if (obj1.equals(obj2)) {
            Assert.fail("did not expect values to be equal (" + obj1.toString()
            + ")");
        }
    }

    /** Asserts that two booleans are not the same */
    public void assertNotEquals(boolean b1, boolean b2) {
        assertNotEquals(new Boolean(b1), new Boolean(b2));
    }

    /** Sleeps for the specified number of milliseconds */
    public void pause(int millisecs) {
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException e) {/* ignored */
        }
    }

    // **** A few helper methods... **** //

    /**
     * Returns a list of selected HtmlOption elements in the Select element
     * located with <code>selectLocator</code>.
     */
    private List<HtmlOption> getSelectedOptions(String selectLocator) {
        HtmlSelect select = (HtmlSelect) getElement(selectLocator);
        if (select == null)
            throw new RuntimeException("Could not find <select> element @ "
                    + selectLocator);
        return select.getSelectedOptions();
    }

    /**
     * Add a Throwable to the lists of verification errors that occured during
     * the test
     *
     * @param e
     *            Any Throwable to be added to verification errors
     */
    private void addToVerificationErrors(Throwable e) {
        verificationErrors.add(e);
    }



    /**
     * [De]selects a given option from a Select element.
     *
     * @param selectLocator
     *            see {@link #select(String, String)}
     * @param optionLocator
     *            see {@link #select(String, String)}
     * @param selected
     *            true if the option should be selected (added to selection),
     *            false if it should be deselected. throws Exception when any
     *            error occurs.
     */
    private void setSelectedOption(final String selectLocator,
            final String optionLocator, final boolean selected) {
        HtmlSelect select = (HtmlSelect) getElement(selectLocator);
        if (select != null) {
            if (optionLocator != null) {

                String option = optionLocator;
                HtmlOption optionToSelect = null;
                if (option.startsWith("value=")) {
                    option = option.substring(6);
                    optionToSelect = select.getOptionByValue(option);
                } else if (option.startsWith("id=")) {
                    option = option.substring(3);
                    List<HtmlOption> options = select.getOptions();
                    for (HtmlOption tmp : options) {
                        if (option.equals(tmp.getId())) {
                            optionToSelect = tmp;
                            break;
                        }
                    }
                } else if (option.startsWith("index=")) {
                    option = option.substring(6);
                    int pos = Integer.parseInt(option);
                    optionToSelect = select.getOption(pos);
                }
                // Default case is label!
                else {
                    if (option.startsWith("label=")) {
                        option = option.substring(6);
                    }
                    List<HtmlOption> options = select.getOptions();
                    for (HtmlOption tmp : options) {
                        if (option.equals(tmp.asText())) {
                            optionToSelect = tmp;
                            break;
                        }
                    }
                }
                if (optionToSelect != null) {
                    select.setSelectedAttribute(
                            optionToSelect, selected);
                } else {
                    throw new RuntimeException(
                            "[de]select failed, optionLocator doesn't match any option for locator = "
                            + selectLocator + ", optionLocator = "
                            + option);
                }
            } else {
                throw new RuntimeException(
                        "[de]select failed, optionLocator is null for locator = "
                        + selectLocator);
            }
        } else {
            throw new RuntimeException(
                    "[de]select failed, cannot find element @ locator = "
                    + selectLocator);
        }
    }

    public List<? extends Object> getElements(final String locator) {
        // Fix selenium-ide's simplified 'id locators', where the locator
        // contains
        // a bare id or name and is not an actual XPath expression.

        // 1. Try with an ID.
        String theLocator = locator;
        boolean bareLocator = false;
        if (!(theLocator.charAt(0) == '/')) {
            bareLocator = true;
            theLocator = "//*[@id='" + locator + "']";
        }
        List<? extends Object> result = getPage().getByXPath(theLocator);
        if (bareLocator && (result == null || result.size() == 0)) {
            // 2. No matching elements with modified locator, try again with a
            // name-based locator.
            theLocator = "//*[@name='" + locator + "']";

            result = getPage().getByXPath(theLocator);
        }
        if (result == null || result.size() == 0) {
            debugElementLocationFailed(locator);
            throw new RuntimeException(
                    "Failed to locate elements with locator " + locator);
        }
        return result;

    }

    public HtmlElement getElement ( String locator) {
        // Fix selenium-ide's simplified 'id locators', where the locator
        // contains
        // a bare id and is not an actual XPath expression.

        // 1. Try with an ID.
        if (locator.startsWith("link=")){
            locator = locator.substring(5);
        }
        String theLocator = locator;
        boolean bareLocator = false;
        if (!(theLocator.charAt(0) == '/')) {
            bareLocator = true;
            theLocator = "//*[@id='" + locator + "']";
        }
        HtmlElement result = (HtmlElement) getPage().getFirstByXPath(theLocator);
        if (bareLocator && result == null) {
            // 2. No matching element with modified locator, try again with a
            // name-based locator.
            theLocator = "//*[@name='" + locator + "']";
            result = (HtmlElement) getPage().getFirstByXPath(theLocator);
        }
        if (bareLocator && result == null) {
            // 2. No matching element with modified locator, try again with a
            // name-based locator.
            theLocator = "//*[contains(text(),'"+locator+"')]";
            result = (HtmlElement) getPage().getFirstByXPath(theLocator);
        }
        if (result == null) {
            debugElementLocationFailed(locator);
            throw new RuntimeException("Failed to locate element with locator "
                    + locator);
        }
        return result;
    }

    private void debugElementLocationFailed(String locator) {
        if (debugPageOnFailure && logger.isLoggable(Level.SEVERE)) {
            logger.severe("====[ FAILURE REPORT  ]====\n" + "Page title: "
                    + getPage().getTitleText() + "\n" + "Window name: "
                    + webClient.getCurrentWindow().getName() + "\n"
                    + "Locator: " + locator + "\n" + "Page content:"
                    + getPage().asXml() + "\n" + "===========================\n");
        }
    }

    /**
     * Specifies if the verify actions should act as assert actions and trigger
     * an exception as soon as they fail.
     *
     * @param verifyActAsAssert
     */
    public void setVerifyActAsAssert(boolean asAssert) {
        verifyActAsAssert = asAssert;
    }

    public String getVerificationErrors(){
        StringBuffer sb = new StringBuffer();
        for (Throwable tr : verificationErrors){
            sb.append(tr.getMessage());
        }
        return sb.toString();
    }

    public void checkForVerificationErrors()throws  java.lang.Exception{
        if (verificationErrors.size() != 0){
            String error = "There are verification errors: \n"
                     + getVerificationErrors();
            verificationErrors.clear();
            throw new java.lang.Exception (error);
        }
    }


    /**
     * Utility method to print a String to a random file on the disk.
     *
     * @param str
     */
    public String printStringToFile(String str) {
        try {
            File tmp = File.createTempFile("loadTestDebugResult", ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
            writer.append(str);
            writer.close();
            return "file://" + tmp.getAbsolutePath();
        } catch (Exception t) {
            t.printStackTrace();
        }
        return "FAILED";
    }

    /**
     * Gets the value of an element attribute.
     *
     * @param attributeLocator
     * an element locator followed by an @ sign and then the name of the
     *            attribute, e.g. "foo@bar"
     * @return the value of the specified attribute
     */
    public String getAttribute(String attributeLocator) {
        int index = attributeLocator.lastIndexOf("@");
        if (index == -1)
            throw new RuntimeException(
                    "Cannot find attibute in xpath locator. Sould be of the form foo@bar. Xpath = '"
                    + attributeLocator + "'.");

        HtmlElement element = getElement(attributeLocator.substring(0, index));
        return element.getAttribute(attributeLocator.substring(index + 1));
    }

    /**
     * Specifies the amount of time that Selenium will wait for actions to complete.
     * <p>
     * Actions that require waiting include "open" and the "waitFor*" actions.
     * </p>
     * The default timeout is 30 seconds.
     *
     * @param timeout a timeout in milliseconds, after which the action will return with an error
     */
    public void setTimeout(String timeout) {
        try {
            Integer timeoutInt = new Integer(timeout);
            getWebClient().setTimeout(timeoutInt);
        } catch(NumberFormatException e) {
            throw new RuntimeException("setTimeout: Cannot set timeout with non integer argument '"+timeout+"'");
        }
    }

    // ***** UNIMPLEMENTED METHODS BELOW.... *****

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#altKeyDown()
         */
    public void altKeyDown() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: altKeyDown");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#altKeyUp()
         */
    public void altKeyUp() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: altKeyUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#answerOnNextPrompt(java.lang.String)
         */
    public void answerOnNextPrompt(String answer) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: answerOnNextPrompt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#captureScreenshot(java.lang.String)
         */
    public void captureScreenshot(String filename) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: captureScreenshot");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#chooseCancelOnNextConfirmation()
         */
    public void chooseCancelOnNextConfirmation() {
        // TODO Auto-generated method stub
        logger
                .warning("Action not implemented: chooseCancelOnNextConfirmation");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#chooseOkOnNextConfirmation()
         */
    public void chooseOkOnNextConfirmation() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: chooseOkOnNextConfirmation");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#clickAt(java.lang.String,
         *      java.lang.String)
         */
    public void clickAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: clickAt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#close()
         */
    public void close() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: close");

    }
        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#controlKeyDown()
         */
    public void controlKeyDown() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: controlKeyDown");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#controlKeyUp()
         */
    public void controlKeyUp() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: controlKeyUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#createCookie(java.lang.String,
         *      java.lang.String)
         */
    public void createCookie(String nameValuePair, String optionsString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: createCookie");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#deleteCookie(java.lang.String,
         *      java.lang.String)
         */
    public void deleteCookie(String name, String path) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: deleteCookie");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#doubleClick(java.lang.String)
         */
    public void doubleClick(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: doubleClick");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#doubleClickAt(java.lang.String,
         *      java.lang.String)
         */
    public void doubleClickAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: doubleClickAt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#dragAndDrop(java.lang.String,
         *      java.lang.String)
         */
    public void dragAndDrop(String locator, String movementsString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: dragAndDrop");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#dragAndDropToObject(java.lang.String,
         *      java.lang.String)
         */
    public void dragAndDropToObject(String locatorOfObjectToBeDragged,
            String locatorOfDragDestinationObject) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: dragAndDropToObject");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#dragdrop(java.lang.String,
         *      java.lang.String)
         */
    public void dragdrop(String locator, String movementsString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: dragdrop");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#fireEvent(java.lang.String,
         *      java.lang.String)
         */
    public void fireEvent(String locator, String eventName) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: fireEvent");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAlert()
         */
    public String getAlert() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAlert");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllButtons()
         */
    public String[] getAllButtons() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllButtons");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllFields()
         */
    public String[] getAllFields() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllFields");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllLinks()
         */
    public String[] getAllLinks() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllLinks");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllWindowIds()
         */
    public String[] getAllWindowIds() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllWindowIds");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllWindowNames()
         */
    public String[] getAllWindowNames() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllWindowNames");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAllWindowTitles()
         */
    public String[] getAllWindowTitles() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAllWindowTitles");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getAttributeFromAllWindows(java.lang.String)
         */
    public String[] getAttributeFromAllWindows(String attributeName) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getAttributeFromAllWindows");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getBodyText()
         */
    public String getBodyText() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getBodyText");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getConfirmation()
         */
    public String getConfirmation() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getConfirmation");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getCookie()
         */
    public String getCookie() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getCookie");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getCursorPosition(java.lang.String)
         */
    public Number getCursorPosition(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getCursorPosition");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getElementHeight(java.lang.String)
         */
    public Number getElementHeight(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getElementHeight");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getElementIndex(java.lang.String)
         */
    public Number getElementIndex(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getElementIndex");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getElementPositionLeft(java.lang.String)
         */
    public Number getElementPositionLeft(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getElementPositionLeft");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getElementPositionTop(java.lang.String)
         */
    public Number getElementPositionTop(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getElementPositionTop");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getElementWidth(java.lang.String)
         */
    public Number getElementWidth(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getElementWidth");
        return null;

    }

        /*
         * Llara: Implemented for assertEval :-)
         *
         * @see com.thoughtworks.selenium.Selenium#getEval(java.lang.String)
         */
    public String getEval(String script) {
        // TODO Auto-generated method stub
        Object obj = webClient.getJavaScriptEngine().execute(this.getPage(),script,"title",0);
        return obj.toString();

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getExpression(java.lang.String)
         */
    public String getExpression(String expression) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getExpression");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getLocation()
         */
    public String getLocation() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getLocation");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getMouseSpeed()
         */
    public Number getMouseSpeed() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getMouseSpeed");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getPrompt()
         */
    public String getPrompt() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getPrompt");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getSpeed()
         */
    public String getSpeed() {
        return ""+delay;

    }

    public String getLog() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getLog");
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see com.thoughtworks.selenium.Selenium#getTable(java.lang.String)
    */
    public String getTable(String tableCellAddress) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getTable");
        return null;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getWhetherThisFrameMatchFrameExpression(java.lang.String,
         *      java.lang.String)
         */
    public boolean getWhetherThisFrameMatchFrameExpression(
            String currentFrameString, String target) {
        // TODO Auto-generated method stub
        logger
                .warning("Action not implemented: getWhetherThisFrameMatchFrameExpression");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getWhetherThisWindowMatchWindowExpression(java.lang.String,
         *      java.lang.String)
         */
    public boolean getWhetherThisWindowMatchWindowExpression(
            String currentWindowString, String target) {
        // TODO Auto-generated method stub
        logger
                .warning("Action not implemented: getWhetherThisWindowMatchWindowExpression");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#getXpathCount(java.lang.String)
         */
    public Number getXpathCount(String xpath) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getXpathCount");
        return null;

    }

    public Number getCssCount(String s) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getCssCount");
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see com.thoughtworks.selenium.Selenium#goBack()
    */
    public void goBack() {
        try {
            currentPage = webClient.getPage(history.pop());
        } catch (Exception e){
            throw new RuntimeException(
                    "Go Back:Unable to Open URL",e);
        }
    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#highlight(java.lang.String)
         */
    public void highlight(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: highlight");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isAlertPresent()
         */
    public boolean isAlertPresent() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isAlertPresent");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isConfirmationPresent()
         */
    public boolean isConfirmationPresent() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isConfirmationPresent");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isEditable(java.lang.String)
         */
    public boolean isEditable(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isEditable");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isOrdered(java.lang.String,
         *      java.lang.String)
         */
    public boolean isOrdered(String locator1, String locator2) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isOrdered");
        return false;

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isPromptPresent()
         */
    public boolean isPromptPresent() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isPromptPresent");
        return false;
    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#isVisible(java.lang.String)
         */
    public boolean isVisible(String locator) {
        HtmlElement element;
        try {
            element=  getElement(locator);
        } catch (Exception e) {
            /* ignored: this is one case where it is legal
             to fail to locate an element */
            return false;
        }
        //Checking all hidden options
        if (element instanceof HtmlHiddenInput){
            return false;
        }
        Map<String, DomAttr> attrs = element.getAttributesMap();
        for (String attr :attrs.keySet()){
            DomAttr domAttr = attrs.get(attr);
            String value = domAttr.getValue();
            if (value.matches(".*display: *none.*")){
                return false;
            }
            if (value.contains(".*visibility: *hidden.*")){
                return false;
            }
        }
        return true;
    }



        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#keyDown(java.lang.String,
         *      java.lang.String)
         */
    public void keyDown(String locator, String keySequence) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyDown");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#keyPress(java.lang.String,
         *      java.lang.String)
         */
    public void keyPress(String locator, String keySequence) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyPress");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#keyUp(java.lang.String,
         *      java.lang.String)
         */
    public void keyUp(String locator, String keySequence) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#metaKeyDown()
         */
    public void metaKeyDown() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: metaKeyDown");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#metaKeyUp()
         */
    public void metaKeyUp() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: metaKeyUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#mouseDownAt(java.lang.String,
         *      java.lang.String)
         */
    public void mouseDownAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseDownAt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#mouseMove(java.lang.String)
         */
    public void mouseMove(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseMove");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#mouseMoveAt(java.lang.String,
         *      java.lang.String)
         */
    public void mouseMoveAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseMoveAt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#mouseUpAt(java.lang.String,
         *      java.lang.String)
         */
    public void mouseUpAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseUpAt");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#openWindow(java.lang.String,
         *      java.lang.String)
         */
    public void openWindow(String url, String windowID) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: openWindow");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#refresh()
         */
    public void refresh() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: refresh");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#setBrowserLogLevel(java.lang.String)
         */
    public void setBrowserLogLevel(String logLevel) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: setBrowserLogLevel");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#setContext(java.lang.String)
         */
    public void setContext(String context) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: setContext");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#setCursorPosition(java.lang.String,
         *      java.lang.String)
         */
    public void setCursorPosition(String locator, String position) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: setCursorPosition");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#setMouseSpeed(java.lang.String)
         */
    public void setMouseSpeed(String pixels) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: setMouseSpeed");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#setSpeed(java.lang.String)
         */
    public void setSpeed(String value) {
        delay = Integer.parseInt(value);
        sleep();

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#shiftKeyDown()
         */
    public void shiftKeyDown() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: shiftKeyDown");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#shiftKeyUp()
         */
    public void shiftKeyUp() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: shiftKeyUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#start()
         */
    public void start() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: start");

    }



        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#submit(java.lang.String)
         */
    public void submit(String formLocator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: submit");

    }

    public void open(String s, String s1) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: open");
    }

    /*
    * (non-Javadoc)
    *
    * @see com.thoughtworks.selenium.Selenium#typeKeys(java.lang.String,
    *      java.lang.String)
    */
    public void typeKeys(String locator, String value) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: typeKeys");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#waitForFrameToLoad(java.lang.String,
         *      java.lang.String)
         */
    public void waitForFrameToLoad(String frameAddress, String timeout) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: waitForFrameToLoad");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#waitForPopUp(java.lang.String,
         *      java.lang.String)
         */
    public void waitForPopUp(String windowID, String timeout) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: waitForPopUp");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#windowFocus()
         */
    public void windowFocus() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: windowFocus");

    }

        /*
         * (non-Javadoc)
         *
         * @see com.thoughtworks.selenium.Selenium#windowMaximize()
         */
    public void windowMaximize() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: windowMaximize");

    }

    public void setExtensionJs(String extensionJs) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: setExtensionJs");
    }

    public void start(String optionsString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: start");
    }

    public void start(Object optionsObject) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: start");
    }

    public void showContextualBanner() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: showContextualBanner");
    }

    public void showContextualBanner(String className, String methodName) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: showContextualBanner");
    }

    public void contextMenu(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: contextMenu");
    }

    public void contextMenuAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: contextMenuAt");
    }

    public void focus(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: focus");
    }

    public void mouseDownRightAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseDownRightAt");
    }

    public void mouseUpRight(String locator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseUpRight");
    }

    public void mouseUpRightAt(String locator, String coordString) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: mouseUpRightAt");
    }

    public void selectPopUp(String windowID) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: selectPopUp");
    }

    public void deselectPopUp() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: deselectPopUp");
    }

    public void assignId(String locator, String identifier) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: assignId");
    }

    public void allowNativeXpath(String allow) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: allowNativeXpath");
    }

    public void ignoreAttributesWithoutValue(String ignore) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: ignoreAttributesWithoutValue");
    }

    public String getCookieByName(String name) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: getCookieByName");
        return null;
    }

    public boolean isCookiePresent(String name) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: isCookiePresent");
        return false;
    }

    public void deleteAllVisibleCookies() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: deleteAllVisibleCookies");
    }

    public void runScript(String script) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: runScript");
    }

    public void addLocationStrategy(String strategyName, String functionDefinition) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: addLocationStrategy");
    }

    public void captureEntirePageScreenshot(String filename, String kwargs) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: captureEntirePageScreenshot");
    }

    public void rollup(String rollupName, String kwargs) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: rollup");
    }

    public void addScript(String scriptContent, String scriptTagId) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: addScript");
    }

    public void removeScript(String scriptTagId) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: removeScript");
    }

    public void useXpathLibrary(String libraryName) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: useXpathLibrary");
    }

    public void attachFile(String fieldLocator, String fileLocator) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: attachFile");
    }

    public String captureScreenshotToString() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: captureScreenshotToString");
        return null;
    }

    public String captureNetworkTraffic(String type) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: captureNetworkTraffic");
        return null;
    }

    public void addCustomRequestHeader(String key, String value) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: addCustomRequestHeader");
    }

    public String captureEntirePageScreenshotToString(String kwargs) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: captureEntirePageScreenshotToString");
        return null;
    }

    public void shutDownSeleniumServer() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: shutDownSeleniumServer");
    }

    public String retrieveLastRemoteControlLogs() {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: retrieveLastRemoteControlLogs");
        return null;
    }

    public void keyDownNative(String keycode) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyDownNative");
    }

    public void keyUpNative(String keycode) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyUpNative");
    }

    public void keyPressNative(String keycode) {
        // TODO Auto-generated method stub
        logger.warning("Action not implemented: keyPressNative");
    }

    public void storeEval(String value,String target) throws Exception{
        getEval(value);
    }


    public void assertAlert(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAlert( ));
    }


    public void assertAlertNotPresent(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAlert( ));
    }

    public void assertAlertPresent(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAlert( ));
    }

    public void assertAllButtons(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllButtons( ));
    }

    public void assertAllFields(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllFields( ));
    }

    public void assertNotTable(String TableCellAddress, String pattern) throws java.lang.Exception{
        assertNotEquals(pattern, getTable(TableCellAddress));
    }

    public void pause(String target) throws java.lang.Exception{
        Thread.sleep(Integer.parseInt(target));
    }

    public void assertAllLinks(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllLinks( ));
    }

    public void assertAllWindowsIds(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllWindowIds( ));
    }

    public void assertAllWindowsNames(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllWindowNames( ));
    }

    public void assertAllWindowsTitles(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getAllWindowTitles( ));
    }

    public void assertBodyText(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getBodyText( ));
    }

    public void assertConfirmation(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getConfirmation( ));
    }

    public void assertHtmlSource(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getHtmlSource( ));
    }

    public void assertLocation(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getLocation( ));
    }


    public void assertMouseSpeed(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getMouseSpeed( ));
    }

    public void assertPrompt(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getPrompt( ));
    }


    public void assertCookie(String pattern) throws java.lang.Exception {
        assertEquals(pattern, getCookie( ));
    }

    public static void assertNotSame(Object obj1,Object obj2){
        Assert.assertNotSame(obj1,obj2);
    }

    public void assertNotAlert(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAlert( ));
    }

    public void assertNotAllButtons(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllButtons( ));
    }

    public void assertNotAllFields(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllFields( ));
    }

    public void assertNotAllLinks(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllLinks( ));
    }

    public void assertNotAllWindowsIds(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllWindowIds( ));
    }

    public void assertNotAllWindowsNames(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllWindowNames( ));
    }

    public void assertNotAllWindowsTitles(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getAllWindowTitles( ));
    }

    public void assertNotBodyText(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getBodyText( ));
    }

    public void assertNotConfirmation(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getConfirmation( ));
    }

    public void assertNotCookie(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getCookie( ));
    }


    public void assertNotHtmlSource(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getHtmlSource( ));
    }

    public void assertNotLocation(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getLocation( ));
    }



    public void assertNotMouseSpeed(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getMouseSpeed( ));
    }

    public void assertNotPrompt(String pattern) throws java.lang.Exception {
        assertNotSame(pattern, getPrompt( ));
    }


    public void assertChecked(String locator) throws java.lang.Exception {
        assertTrue(isChecked(locator));
    }

    public void assertEditable(String locator) throws java.lang.Exception {
        assertTrue(isEditable(locator));
    }

    public void assertElementPresent(String locator) throws java.lang.Exception {
        assertTrue(isElementPresent(locator));
    }

    public void assertVisible(String locator) throws java.lang.Exception {
        assertTrue(isVisible(locator));
    }
    public void assertNotChecked(String locator) throws java.lang.Exception {
        assertFalse(isChecked(locator));
    }

    public void assertNotEditable(String locator) throws java.lang.Exception {
        assertFalse(isEditable(locator));
    }

    public void assertElementNotPresent(String locator) throws java.lang.Exception {
        assertFalse(isElementPresent(locator));
    }


    public void assertSelectOptions(String pattern,String[] selectLocator) throws java.lang.Exception {
        String[] array = getSelectOptions(pattern);
        assertEquals(selectLocator.length,array.length);
        for (int i = 0; i < array.length; i++){
            assertEquals(selectLocator[i],array[i]);
        }
    }

    public void assertNotSelectOptions(String pattern,String[] selectLocator) throws java.lang.Exception {
        String[] array = getSelectOptions(pattern);
        assertNotSame(selectLocator.length,array.length);
        for (int i = 0; i < array.length; i++){
            assertNotSame(selectLocator[i],array[i]);
        }
    }


    public void assertSelectedId(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedId(pattern));
    }

    public void assertSelectedIds(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedIds(pattern));
    }

    public void assertSelectedIndex(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedIndex(pattern));
    }

    public void assertSelectedIndexes(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedIndexes(pattern));
    }

    public void assertSelectedLabel(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedLabel(pattern));
    }

    public void assertSelectedLabels(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedLabels(pattern));
    }

    public void assertSelectedValue(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedValue(pattern));
    }

    public void assertSelectedValues(String selectLocator,String pattern) throws java.lang.Exception {
        assertEquals(selectLocator,getSelectedValues(pattern));
    }
    public void assertNotSelectedId(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedId(pattern));
    }

    public void assertNotSelectedIds(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedIds(pattern));
    }

    public void assertNotSelectedIndex(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedIndex(pattern));
    }

    public void assertNotSelectedIndexes(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedIndexes(pattern));
    }

    public void assertNotSelectedLabel(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedLabel(pattern));
    }

    public void assertNotSelectedLabels(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedLabels(pattern));
    }


    public void assertNotSelectedValue(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedValue(pattern));
    }

    public void assertNotSelectedValues(String selectLocator,String pattern) throws java.lang.Exception {
        assertNotSame(selectLocator,getSelectedValues(pattern));
    }
    public void assertCursorPosition(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getCursorPosition(pattern));
    }

    public void assertElementHeight(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getElementHeight(pattern));
    }

    public void assertElementIndex(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getElementIndex(pattern));
    }

    public void assertElementPositionLeft(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getElementPositionLeft(pattern));
    }

    public void assertElementPositionTop(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getElementPositionTop(pattern));
    }

    public void assertElementWidth(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getElementWidth(pattern));
    }

    public void assertText(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getText(pattern));
    }

    public void assertValue(String locator,String pattern) throws java.lang.Exception {
        assertEquals(locator,getValue(pattern));
    }
    public void assertNotCursorPosition(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getCursorPosition(pattern));
    }

    public void assertNotElementHeight(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getElementHeight(pattern));
    }

    public void assertNotElementIndex(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getElementIndex(pattern));
    }

    public void assertNotElementPositionLeft(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getElementPositionLeft(pattern));
    }

    public void assertNotElementPositionTop(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getElementPositionTop(pattern));
    }


    public void assertNotElementWidth(String locator,String pattern) throws java.lang.Exception {
        assertNotSame(locator,getElementWidth(pattern));
    }
    public void assertConfirmationPresent( ) throws java.lang.Exception {
        assertTrue(isConfirmationPresent( ));
    }

    public void assertPromptPresent( ) throws java.lang.Exception {
        assertTrue(isPromptPresent( ));
    }
    public void assertConfirmationNotPresent( ) throws java.lang.Exception {
        assertFalse(isConfirmationPresent( ));
    }


   public void assertPromptNotPresent( ) throws java.lang.Exception {
        assertFalse(isPromptPresent( ));
    }
    public void assertAttribute(String attributeLocator,String pattern) throws java.lang.Exception {
        assertEquals(attributeLocator,getAttribute(pattern));
    }
    public void assertAttributeFromAllWindows(String[] attributeName, String pattern) throws java.lang.Exception {
        String[] array = getAttributeFromAllWindows(pattern);
        assertEquals(attributeName.length,array.length);
        for (int i = 0; i < array.length; i++){
            assertEquals(attributeName [i],array[i]);
        }
    }

    public void assertNotAttribute(String attributeLocator,String pattern) throws java.lang.Exception {
        assertNotSame(attributeLocator,getAttribute(pattern));
    }

    public void assertNotAttributeFromAllWindows(String[] attributeName, String pattern) throws java.lang.Exception {
        String[] array = getAttributeFromAllWindows(pattern);
        assertEquals(attributeName.length,array.length);
        for (int i = 0; i < array.length; i++){
            assertEquals(attributeName [i],array[i]);
        }
    }

    public void assertNotEval(String script,String pattern) throws java.lang.Exception {
        assertNotSame(script,getEval(pattern));
    }

    public void assertNotExpression(String script,String pattern) throws java.lang.Exception {
        assertNotSame(script,getExpression(pattern));
    }

    public void assertNotOrdered(String locator1,String locator2) throws java.lang.Exception {
        assertFalse(isOrdered(locator1,locator2));
    }

    public void assertOrdered(String locator1, String locator2) throws java.lang.Exception {
        assertTrue(isOrdered(locator1,locator2));
    }

    public void assertNotSomethingSelected(String selectLocator) throws java.lang.Exception {
        assertFalse(isSomethingSelected(selectLocator));
    }

    public void assertSomethingSelected(String selectLocator) throws java.lang.Exception {
        assertTrue(isSomethingSelected(selectLocator));
    }

    public void assertTable(String tableCellAddress,String pattern) throws java.lang.Exception {
        assertEquals(tableCellAddress,getTable(pattern));
    }

    public void verifyAlert(String pattern) throws Exception {
        verifyEquals(pattern, getAlert());
    }

    public void verifyAlertNotPresent() throws Exception {
        verifyFalse(isAlertPresent());
    }

    public void verifyAlertPresent() throws Exception {
        verifyTrue(isAlertPresent());
    }

    public void verifyAllButtons(String pattern) throws Exception {
        String[] array = getAllButtons();
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifyAllFields(String pattern)throws Exception {
        String[] array = getAllFields();
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifyAllLinks (String pattern )throws Exception {
        String[] array = getAllLinks();
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifyAllWindowIds(String pattern )throws Exception {
        String[] array = getAllWindowIds();
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifyAllWindowNames(String pattern )throws Exception {
        String[] array = getAllWindowNames();
	verifyEquals(1, array.length);
	verifyEquals(pattern, array[0]);
    }

    public void verifyAllWindowTitles(String pattern )throws Exception {
        String[] array = getAllWindowTitles();
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifyAttribute (String attributeLocator,String pattern) throws Exception{
        verifyEquals(pattern, getAttribute(attributeLocator));
    }

    public void verifyAttributeFromAllWindows (String attributeLocator,String pattern) throws Exception{
        String[] array = getAttributeFromAllWindows(attributeLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }


    public void verifyBodyText(String pattern )throws Exception {
        verifyEquals(pattern, getBodyText());
    }

    public void verifyChecked(String locator)throws Exception{
        verifyTrue(isChecked(locator));
    }

    public void verifyConfirmation(String pattern )throws Exception {
        verifyEquals(pattern, getConfirmation());
    }

    public void verifyConfirmationNotPresent(String pattern )throws Exception {
        verifyFalse(isConfirmationPresent());
    }

    public void verifyConfirmationPresent(String pattern )throws Exception {
        verifyTrue(isConfirmationPresent());
    }

    public void verifyCookie(String pattern )throws Exception {
        verifyEquals(pattern, getCookie());
    }


    public void verifyCursorPosition (String locator,String pattern)throws Exception {
        verifyEquals(pattern, getCursorPosition(locator));
    }

    public void verifyEditable(String locator)throws Exception {
        verifyTrue(isEditable(locator));
    }

    public void verifyElementHeight(String locator,String pattern) throws Exception {
        verifyEquals(pattern, getElementHeight(locator));
    }

    public void verifyElementIndex(String locator,String pattern) throws Exception {
        verifyEquals(pattern, getElementIndex(locator));
    }

    public void verifyElementNotPresent(String locator) throws Exception {
        verifyFalse(isElementPresent(locator));
    }

    public void verifyElementPositionLeft(String locator,String pattern) throws Exception {
        verifyEquals(pattern, getElementPositionLeft(locator));
    }

   public void verifyElementPositionTop(String locator,String pattern) throws Exception {
        verifyEquals(pattern, getElementPositionTop(locator));
    }

    public void verifyElementPresent(String locator) throws Exception {
        verifyTrue(isElementPresent(locator));
    }

    public void verifyElementWidth (String locator,String pattern) throws Exception {
        verifyEquals(pattern, getElementWidth(locator));
    }

    public void verifyEval (String script,String pattern)throws Exception  {
        verifyEquals(pattern, getEval(script));
    }

    public void verifyExpression(String expression,String pattern) throws Exception {
        verifyEquals(pattern, getExpression(expression));
    }

    public void verifyHtmlSource (String pattern) throws Exception {
        verifyEquals(pattern, getHtmlSource());
    }

    public void verifyLocation (String pattern) throws Exception {
        verifyEquals(pattern,getLocation());
    }

    public void verifyMouseSpeed(String pattern) throws Exception {
        verifyEquals(pattern, getMouseSpeed());
    }

    public void verifyNotEquals(String a,String b) throws Exception {
        Assert.assertNotSame(a,b);
    }



    public void verifyNotAlert (String pattern)throws Exception {
        verifyNotEquals(pattern, getAlert());
    }

    public void verifyNotAllButtons(String pattern)throws Exception {
        String[] array = getAllButtons();
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotAllFields(String pattern)throws Exception {
        String[] array = getAllFields();
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }


    public void verifyNotAllLinks(String pattern)throws Exception {
        String[] array1 = getAllLinks();
        if (1 == array1.length) {
            verifyFalse(pattern.equals(array1[0]));
        }
    }

    public void verifyNotAllWindowIds(String pattern)throws Exception {
        String[] array = getAllWindowIds();
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotAllWindowNames(String pattern)throws Exception {
        String[] array = getAllWindowNames();
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotAllWindowTitles(String pattern)throws Exception {
        String[] array = getAllWindowTitles();
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotAttribute (String attributeLocator,String pattern) throws Exception {
        verifyNotEquals(pattern, getAttribute(attributeLocator));
    }

    public void verifyNotAttributeFromAllWindows
            (String attributeName,String pattern) throws Exception {
        String[] array = getAttributeFromAllWindows(attributeName);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotBodyText (String pattern) throws Exception {
        verifyNotEquals(pattern, getBodyText());
    }

    public void verifyNotChecked (String locator) throws Exception {
        verifyFalse(isChecked(locator));
    }

    public void verifyNotConfirmation (String pattern) throws Exception {
        verifyNotEquals(pattern, getConfirmation());
    }

    public void verifyNotCookie (String pattern)throws Exception {
        verifyNotEquals(pattern, getCookie());
    }


    public void verifyNotCursorPosition (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getCursorPosition(locator));
    }

    public void verifyNotEditable (String locator)throws Exception{
        verifyFalse(isEditable(locator));
    }

    public void verifyNotElementHeight(String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getElementHeight(locator));
    }

    public void verifyNotElementIndex (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getElementIndex(locator));
    }

    public void verifyNotElementPositionLeft
            (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getElementPositionLeft(locator));
    }

    public void verifyNotElementPositionTop
            (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getElementPositionTop(locator));
    }

    public void verifyNotElementWidth
            (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getElementWidth(locator));
    }

    public void verifyNotEval
            (String script,String pattern)throws Exception{
        verifyNotEquals(pattern, getEval(script));
    }

    public void verifyNotExpression
            (String expression,String pattern)throws Exception{
        verifyNotEquals(pattern, getExpression(expression));
    }

    public void verifyNotHtmlSource(String pattern)throws Exception{
        verifyNotEquals(pattern,  getHtmlSource());
    }

    public void verifyNotLocation (String pattern)throws Exception{
        verifyNotEquals(pattern, getLocation());
    }

    public void verifyNotMouseSpeed(String pattern)throws Exception{
        verifyNotEquals(pattern, getMouseSpeed());
    }

    public void verifyNotOrdered (String locator1, String locator2)throws Exception{
        verifyFalse(isOrdered(locator1, locator2));
    }

    public void verifyNotPrompt (String pattern)throws Exception{
        verifyNotEquals(pattern, getPrompt());
    }

    public void verifyNotSelectOptions
            (String selectLocator,String pattern)throws Exception{
        String[] array = getSelectOptions(selectLocator);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotSelectedId
            (String selectLocator,String pattern)throws Exception{
        verifyNotEquals(pattern, getSelectedId(selectLocator));
    }

    public void verifyNotSelectedIds
            (String selectLocator,String pattern)throws Exception{
        String[] array = getSelectedIds(selectLocator);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotSelectedIndex
             (String selectLocator,String pattern)throws Exception{
        verifyNotEquals(pattern, getSelectedIndex(selectLocator));
    }

    public void verifyNotSelectedIndexes
             (String selectLocator,String pattern)throws Exception{
        String[] array = getSelectedIndexes(selectLocator);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotSelectedLabel
            (String selectLocator,String pattern)throws Exception{
        verifyNotEquals(pattern, getSelectedLabel(selectLocator));
    }

    public void verifyNotSelectedLabels
            (String selectLocator,String pattern)throws Exception{
        String[] array = getSelectedLabels(selectLocator);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotSelectedValue
            (String selectLocator,String pattern)throws Exception{
        verifyNotEquals(pattern, getSelectedValue(selectLocator));
    }


    public void verifyNotSelectedValues
            (String selectLocator,String pattern)throws Exception{
        String[] array= getSelectedValues(selectLocator);
        if (1 == array.length) {
            verifyFalse(pattern.equals(array[0]));
        }
    }

    public void verifyNotSomethingSelected
            (String selectLocator)throws Exception{
        verifyFalse(isSomethingSelected(selectLocator));
    }

    public void fail (){
        junit.framework.TestCase.fail();
    }
    public void fail (String msg){
        junit.framework.TestCase.fail(msg);
    }


    public void verifyNotTable
            (String tableCellAddress,String pattern)throws Exception{
        verifyNotEquals(pattern, getTable(tableCellAddress));
    }

    public void verifyNotText
            (String selectLocator,String pattern)throws Exception{
        verifyNotEquals(pattern, getText(selectLocator));
    }

    public void verifyNotTitle
            (String pattern)throws Exception{
        verifyNotEquals(pattern, getTitle());
    }

    public void verifyNotValue
            (String locator,String pattern)throws Exception{
        verifyNotEquals(pattern, getValue(locator));
    }

    public void verifyNotVisible
            (String locator) throws Exception {
        verifyFalse(isVisible(locator));
    }

    public void verifyNotXpathCount
            (String xpath,String pattern) throws Exception {
        verifyNotEquals(pattern, getXpathCount(xpath));
    }

    public void verifyOrdered
            (String locator1,String locator2)  throws Exception {
        verifyTrue(isOrdered(locator1, locator2));
    }

    public void verifyPrompt (String pattern)throws Exception{
        verifyEquals(pattern, getPrompt());
    }

    public void verifyPromptNotPresent()throws Exception{
        verifyFalse(isPromptPresent());
    }

    public void verifyPromptPresent () throws Exception{
        verifyTrue(isPromptPresent());
    }

    public void verifySelectOptions
            (String selectLocator,String pattern) throws Exception{
        String[] array = getSelectOptions(selectLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifySelectedId
            (String selectLocator,String pattern) throws Exception{
        verifyEquals(pattern, getSelectedId(selectLocator));
    }


    public void verifySelectedIds
            (String selectLocator,String pattern) throws Exception{
        String[] array = getSelectedIds(selectLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifySelectedIndex
            (String selectLocator,String pattern) throws Exception{
        verifyEquals(pattern, getSelectedIndex(selectLocator));
    }

    public void verifySelectedIndexes
            (String selectLocator,String pattern) throws Exception{
        String[] array = getSelectedIndexes(selectLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifySelectedLabel
            (String selectLocator,String pattern) throws Exception{
        verifyEquals(pattern, getSelectedLabel(selectLocator));
    }

    public void verifySelectedLabels
            (String selectLocator,String pattern) throws Exception{
        String[] array = getSelectedLabels(selectLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifySelectedValue
            (String selectLocator,String pattern) throws Exception{
        verifyEquals(pattern, getSelectedValue(selectLocator));
    }

    public void verifySelectedValues
             (String selectLocator,String pattern) throws Exception{
        String[] array =getSelectedValues(selectLocator);
        verifyEquals(1, array.length);
        verifyEquals(pattern, array[0]);
    }

    public void verifySomethingSelected
            (String selectLocator) throws Exception{
        verifyTrue(isSomethingSelected(selectLocator));
    }

    public void verifyTable
            (String tableCellAddress,String pattern)throws Exception{
        verifyEquals(pattern, getTable(tableCellAddress));
    }

    public void verifyText
            (String locator,String pattern) throws java.lang.Exception {
        verifyEquals(pattern, getText(locator));
    }

    public void verifyTextNotPresent(String pattern) throws java.lang.Exception{
        verifyFalse(isTextPresent(pattern));
    }

    public void verifyTitle (String pattern) throws java.lang.Exception{
        verifyEquals(pattern, getTitle());
    }

    public void verifyValue
            (String locator, String pattern) throws java.lang.Exception{
        verifyEquals(pattern, getValue(locator));
    }

    public void verifyVisible
             (String locator) throws java.lang.Exception{
        verifyTrue( isVisible(locator));
    }

    public void verifyXpathCount
            (String xpath, String pattern) throws java.lang.Exception{
        verifyEquals(pattern, getXpathCount(xpath));
    }

    public void waitForAlert(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (pattern.equals(getAlert())) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAlertNotPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (!isAlertPresent()) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }



    public void waitForAlertPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (isAlertPresent()) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllButtons(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllButtons()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllFields(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllFields()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllLinks(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllLinks()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllWindowIds (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowIds()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllWindowNames (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowNames()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAllWindowTitles (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowTitles()){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAttribute
            (String attributeLocator, String pattern) throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getAttribute(attributeLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForAttributeFromAllWindows
            (String attributeLocator, String pattern) throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAttributeFromAllWindows(attributeLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForBodyText
            ( String pattern) throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getBodyText()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForChecked(String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isChecked(locator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForConfirmation(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getConfirmation()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForConfirmationNotPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isConfirmationPresent())
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForConfirmationPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isConfirmationPresent())
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForCookie(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getCookie()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }




    public void waitForCursorPosition
            (String locator, String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getCursorPosition(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForEditable
            (String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isEditable(locator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForElementHeight
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getElementHeight(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForElementIndex
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getElementIndex(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForElementNotPresent
             (String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isElementPresent(locator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForElementPositionLeft
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getElementPositionLeft(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }


    public void waitForElementPositionTop
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getElementPositionTop(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }


    public void waitForElementWidth
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getElementWidth(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForEval
            (String script,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getEval(script)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForExpression
            (String script,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getExpression("expression")))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForHtmlSource
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getHtmlSource()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForLocation
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getLocation()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForMouseSpeed
             (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getMouseSpeed()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAlert
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getAlert()))
                break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }





    public void waitForNotAllButtons(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllButtons()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAllFields(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllFields()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAllLinks(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllLinks()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAllWindowIds (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowIds()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAllWindowNames (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowNames()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAllWindowTitles (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAllWindowTitles()){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotAttribute
            (String attributeLocator, String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getAttribute(attributeLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }


    public void waitForNotAttributeFromAllWindows
            (String attributeName, String pattern) throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getAttributeFromAllWindows(attributeName)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotBodyText(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getBodyText()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotChecked(String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isChecked(locator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotConfirmation
            (String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!locator.equals(getConfirmation()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotCookie(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getCookie()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }



    public void waitForNotCursorPosition
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getCursorPosition(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotEditable(String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (!isEditable(locator))
                break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotElementHeight
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getElementHeight(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotElementIndex
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getElementIndex(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotElementPositionLeft
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getElementPositionLeft(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotElementPositionTop
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getElementPositionTop(locator))) break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotElementWidth
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getElementWidth(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotEval
            (String script,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getEval(script)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotExpression
            (String expression,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getExpression(expression)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotHtmlSource
             (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getHtmlSource()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotLocation
             (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getLocation()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotMouseSpeed
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getMouseSpeed()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotOrdered
            (String locator1,String locator2)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isOrdered(locator1, locator2))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotPrompt  (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getPrompt()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedId
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getSelectedId(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedIndex
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getSelectedIndex(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedLabel
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getSelectedLabel(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedValue
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getSelectedValue(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSomethingSelected
            (String selectLocator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isSomethingSelected(selectLocator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }


    public void waitForNotTable
            (String tableCellAddress, String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getTable(tableCellAddress)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotText
            (String locator, String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getText(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotTitle
            ( String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getTitle()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotValue
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!pattern.equals(getValue(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotVisible
            (String locator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isVisible(locator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForOrdered
             (String locator1,String locator2)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isOrdered(locator1, locator2))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForPrompt
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getPrompt()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForPromptNotPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isPromptPresent())
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForPromptPresent()throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isPromptPresent())
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedId
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getSelectedId(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedIndex
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getSelectedIndex(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedLabel
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getSelectedLabel(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedValue
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getSelectedValue(selectLocator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSomethingSelected
            (String selectLocator)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isSomethingSelected(selectLocator))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }



    public void waitForTable
            (String tableCellAddress,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getTable(tableCellAddress)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForText
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getText(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForTextNotPresent
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (!isTextPresent(pattern))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForTextPresent
            (String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (isTextPresent(pattern))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForTitle(String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getTitle()))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForValue
            (String locator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getValue(locator)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForVisible(String locator) throws Exception{
        for (int second = 0;; second++) {
            if (second >= 10) fail("timeout");
            try {
                if (isVisible(locator)){
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }

    public void waitForXpathCount(String xpath,String pattern)throws Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                if (pattern.equals(getXpathCount(xpath)))
                    break;
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedIds
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: this.getSelectedIds(selectLocator)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedIds
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedIds(selectLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectOptions
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectOptions(selectLocator)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectOptions
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectOptions(selectLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedIndexes
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedIndexes(selectLocator)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedIndexes
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedIndexes(selectLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedLabels
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedLabels(selectLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedLabels
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedLabels(selectLocator)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForNotSelectedValues
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedValues(selectLocator)){
                    if (!pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void waitForSelectedValues
            (String selectLocator,String pattern)throws java.lang.Exception{
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try {
                for (String button: getSelectedValues(selectLocator)){
                    if (pattern.equals(button))
                        break;
                }
            } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }

    public void controlKeyDownAndWait()throws java.lang.Exception{
        controlKeyDown();
        waitForPageToLoad("30000");
    }

    public void controlKeyUpAndWait()throws java.lang.Exception{
        controlKeyUp();
        waitForPageToLoad("30000");
    }



    public void addSelectionAndWait
            (String locator,String optionLocator)throws java.lang.Exception{
        addSelection(locator, optionLocator);
        waitForPageToLoad("30000");
    }

    public void altKeyDownAndWait()throws java.lang.Exception{
        altKeyDown();
        waitForPageToLoad("30000");
    }


    public void altKeyUpAndWait()throws java.lang.Exception{
        altKeyUp();
        waitForPageToLoad("30000");
    }




    public void checkAndWait(String locator)throws java.lang.Exception{
        check(locator);
        waitForPageToLoad("30000");
    }

    public void chooseOkOnNextConfirmationAndWait()throws java.lang.Exception{
        chooseOkOnNextConfirmation();
        waitForPageToLoad("30000");
    }

    public void removeSelectionAndWait
            (String locator,String optionLocator)throws java.lang.Exception{
        removeSelection(locator, optionLocator);
        waitForPageToLoad("30000");
    }

    public void setSpeedAndWait(String value) throws java.lang.Exception{
        setSpeed(value);
        waitForPageToLoad("30000");
    }

    public void shiftKeyDownAndWait ()throws java.lang.Exception{
        shiftKeyDown();
        waitForPageToLoad("30000");
    }
    public void shiftKeyUpAndWait ()throws java.lang.Exception{
        shiftKeyUp();
        waitForPageToLoad("30000");
    }

    public void uncheckAndWait(String locator)throws java.lang.Exception{
        uncheck(locator);
        waitForPageToLoad("30000");
    }

    public void windowFocusAndWait () throws java.lang.Exception{
        windowFocus();
        waitForPageToLoad("30000");
    }

    public void windowMaximizeAndWait() throws java.lang.Exception{
        windowMaximize();
        waitForPageToLoad("30000");
    }


    public void clickAtAndWait
            (String locator,String  coordString) throws java.lang.Exception{
        clickAt(locator, coordString);
        waitForPageToLoad("30000");
    }

    public void createCookieAndWait
            (String nameValuePair,String optionsString)  throws java.lang.Exception{
        createCookie(nameValuePair, optionsString);
        waitForPageToLoad("30000");
    }


    public void deleteCookieAndWait
            (String name,String optionsString)  throws java.lang.Exception{
        deleteCookie(name, optionsString);
        waitForPageToLoad("30000");
    }

    public void doubleClickAtAndWait
            (String locator,String coordString)  throws java.lang.Exception{
        doubleClickAt(locator, coordString);
        waitForPageToLoad("30000");
    }

    public void dragAndDropAndWait
            (String locator,String movementsString)  throws java.lang.Exception{
        dragAndDrop(locator, movementsString);
        waitForPageToLoad("30000");
    }

    public void dragAndDropToObjectAndWait
            (String locatorOfObjectToBeDragged, String locatorOfDragDestinationObject)  throws java.lang.Exception{
        dragAndDropToObject (locatorOfObjectToBeDragged, locatorOfDragDestinationObject);
        waitForPageToLoad("30000");
    }

    public void dragdropAndWait
            (String locator,String movementsString)  throws java.lang.Exception{
        dragdrop(locator, movementsString);
        waitForPageToLoad("30000");
    }

    public void echo(String message){
        System.out.println(message);
    }

    public void fireEventAndWait
            (String locator,String eventName)  throws java.lang.Exception{
        fireEvent(locator, eventName);
        waitForPageToLoad("30000");
    }


    public void goBackAndWait()  throws java.lang.Exception{
        goBack();
        waitForPageToLoad("30000");
    }

    public void highlightAndWait(String locator)  throws java.lang.Exception{
        highlight(locator);
        waitForPageToLoad("30000");
    }


    public void keyDownAndWait
            (String locator,String keySequence)  throws java.lang.Exception{
        keyDown(locator, keySequence);
        waitForPageToLoad("30000");
    }

    public void keyPressAndWait
            (String locator,String keySequence)  throws java.lang.Exception{
        keyPress(locator, keySequence);
        waitForPageToLoad("30000");
    }

    public void keyUpAndWait
            (String locator,String keySequence)  throws java.lang.Exception{
        keyUp(locator, keySequence);
        waitForPageToLoad("30000");
    }

    public void metaKeyDownAndWait () throws java.lang.Exception{
        metaKeyDown();
        waitForPageToLoad("30000");
    }

    public void metaKeyUpAndWait()  throws java.lang.Exception{
        metaKeyUp();
        waitForPageToLoad("30000");
    }

    public void mouseDownAndWait(String locator)  throws java.lang.Exception{
        mouseDown(locator);
        waitForPageToLoad("30000");
    }

    public void mouseDownAtAndWait
            (String locator,String coordString)  throws java.lang.Exception{
        mouseDownAt(locator, coordString);
        waitForPageToLoad("30000");
    }


    public void mouseMoveAndWait(String locator)  throws java.lang.Exception{
        mouseMove(locator);
        waitForPageToLoad("30000");
    }

    public void mouseMoveAtAndWait
            (String locator,String coordString)  throws java.lang.Exception{
        mouseMoveAt(locator, coordString);
        waitForPageToLoad("30000");
    }

    public void mouseOutAndWait(String locator)  throws java.lang.Exception{
        mouseOut(locator);
        waitForPageToLoad("30000");
    }

    public void mouseOverAndWait(String locator)  throws java.lang.Exception{
        mouseOver(locator);
        waitForPageToLoad("30000");
    }

    public void mouseUpAndWait(String locator)  throws java.lang.Exception{
        mouseUp(locator);
        waitForPageToLoad("30000");
    }

    public void mouseUpAtAndWait
            (String locator,String coordString)  throws java.lang.Exception{
        mouseUpAt( locator,  coordString);
        waitForPageToLoad("30000");
    }


    public void openWindowAndWait(String url,String windowID)  throws java.lang.Exception{
        openWindow(url, windowID);
        waitForPageToLoad("30000");
    }

    public void refreshAndWait()  throws java.lang.Exception{
        refresh();
        waitForPageToLoad("30000");
    }

    public void removeAllSelectionsAndWait
            (String locator)  throws java.lang.Exception{
        removeAllSelections(locator);
        waitForPageToLoad("30000");
    }

    public void selectAndWait
            (String selectLocator,String optionLocator) throws java.lang.Exception{
        select(selectLocator, optionLocator);
        waitForPageToLoad("30000");
    }

    public void setBrowserLogLevelAndWait
            (String logLevel)  throws java.lang.Exception{
        setBrowserLogLevel(logLevel);
        waitForPageToLoad("30000");
    }

    public void setCursorPositionAndWait
            (String locator,String position)  throws java.lang.Exception{
        setCursorPosition( locator, position);
        waitForPageToLoad("30000");
    }

    public void submitAndWait(String formLocator)  throws java.lang.Exception{
        submit(formLocator);
        waitForPageToLoad("30000");
    }

    public void typeAndWait
            (String locator,String value)  throws java.lang.Exception{
        type(locator, value);
        waitForPageToLoad("30000");
    }

    public void typeKeysAndWait
            (String locator,String value)  throws java.lang.Exception{
        typeKeys(locator, value);
        waitForPageToLoad("30000");
    }

    public void setMouseSpeedAndWait(String pixels)  throws java.lang.Exception{
        setMouseSpeed(pixels);
        waitForPageToLoad("30000");
    }

}

// Local Variables:
// compile-command: "ant -emacs -s build.xml install-in-TM"
// End: