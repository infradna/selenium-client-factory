package com.saucelabs.sauce_ondemand.driver;

import com.saucelabs.rest.Credential;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.kohsuke.MetaInfServices;
import org.seleniumhq.selenium.client.factory.SeleniumFactory;
import org.seleniumhq.selenium.client.factory.spi.SeleniumFactorySPI;

import java.io.IOException;
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

        // massage parameter into JSON format
        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
        for (String param : uri.substring(SCHEME.length()).split("&")) {
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

        return new DefaultSelenium("saucelabs.com",4444,buf.toString(),browserURL);
    }

    private static final String SCHEME = "sauce-ondemand:";
}
