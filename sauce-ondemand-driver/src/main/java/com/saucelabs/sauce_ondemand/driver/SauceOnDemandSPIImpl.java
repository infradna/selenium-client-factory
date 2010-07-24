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
package com.saucelabs.sauce_ondemand.driver;

import com.saucelabs.rest.Credential;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link SeleniumFactorySPI} that talks to Sauce OnDemand.
 * 
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class SauceOnDemandSPIImpl extends SeleniumFactorySPI {
    @Override
    public Selenium createSelenium(SeleniumFactory factory, String browserURL) {
        String uri = factory.getUri();
        if (!uri.startsWith(SCHEME))
            return null; // not ours

        uri = uri.substring(SCHEME.length());
        if (!uri.startsWith("?"))
            throw new IllegalArgumentException("Missing '?':"+factory.getUri());

        // massage parameter into JSON format
        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
        for (String param : uri.substring(1).split("&")) {
            int idx = param.indexOf('=');
            if(idx<0)   throw new IllegalArgumentException("Invalid parameter format: "+uri);
            String key = param.substring(0,idx);
            String value = param.substring(idx+1);

            List<String> v = paramMap.get(key);
            if (v==null)    paramMap.put(key, v = new ArrayList<String>());
            v.add(value);
        }

        if (paramMap.get("username")==null && paramMap.get("access-key")==null) {
            try {
                // read the credential from a credential file
                Credential cred = new Credential();
                paramMap.put("username", Collections.singletonList(cred.getUsername()));
                paramMap.put("access-key", Collections.singletonList(cred.getKey()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read "+Credential.getDefaultCredentialFile(),e);
            }
        }

        if (paramMap.get("job-name")==null)
            paramMap.put("job-name",Collections.singletonList(getJobName()));

        return new SeleniumImpl("saucelabs.com",4444, toJSON(paramMap), browserURL);
    }

    /**
     * Try to find the name of the test as best we can.
     */
    public String getJobName() {
        StackTraceElement[] trace = new Exception().getStackTrace();
        boolean foundFactory = false;
        String callerName = null;
        for (StackTraceElement e : trace) {
            if (foundFactory)
                callerName = e.toString();
            foundFactory = e.getClassName().equals(SeleniumFactory.class.getName());
        }

        String vmName = ManagementFactory.getRuntimeMXBean().getName();

        return callerName+" on "+vmName;
    }

    /**
     * Converts a multi-map to a JSON format.
     */
    private String toJSON(Map<String, List<String>> paramMap) {
        boolean first=true;
        StringBuilder buf = new StringBuilder("{");
        for (Entry<String, List<String>> e : paramMap.entrySet()) {
            if (first)  first = false;
            else        buf.append(',');
            buf.append('"').append(e.getKey()).append("\":");

            List<String> v = e.getValue();
            if (v.size()==1) {
                buf.append('"').append(v.get(0)).append('"');
            } else {
                buf.append('[');
                for (int i=0; i<v.size(); i++) {
                    if(i!=0)    buf.append(',');
                    buf.append('"').append(v.get(i)).append('"');
                }
                buf.append(']');
            }
        }
        buf.append('}');
        return buf.toString();
    }

    private static final String SCHEME = "sauce-ondemand:";
}
