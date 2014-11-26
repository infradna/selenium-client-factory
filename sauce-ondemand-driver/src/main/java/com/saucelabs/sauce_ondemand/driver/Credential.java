package com.saucelabs.sauce_ondemand.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Credential for the Sauce OnDemand service.
 *
 * @author Kohsuke Kawaguchi
 */
public class Credential {
    /**
     * User name.
     */
    private final String username;
    /**
     * API access key, which looks like a GUID.
     */
    private final String key;

    /**
     * Creates a credential by specifying the username and the key directly.
     */
    public Credential(String username, String key) {
        this.username = username;
        this.key = key;
    }

    /**
     * Loads a credential from the specified property file.
     *
     * The property file should look like the following:
     * <pre>
     * username=kohsuke
     * password=12345678-1234-1234-1234-1234567890ab
     * </pre>
     *
     * @throws java.io.IOException
     *      If the file I/O fails, such as non-existent file, incorrect format, or if the file is missing
     *      the 'username' or 'key' parameters.
     */
    public Credential(File propertyFile) throws IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(propertyFile);
        try {
            props.load(in);
            this.username = props.getProperty("username");
            this.key = props.getProperty("key");
            if (username==null)
                throw new IOException(propertyFile+" didn't contain the 'username' parameter");
            if (key==null)
                throw new IOException(propertyFile+" didn't contain the 'key' parameter");
        } finally {
            in.close();
        }
    }

    /**
     * Loads the credential from the default location "~/.sauce-ondemand"
     */
    public Credential() throws IOException {
        this(getDefaultCredentialFile());
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    /**
     * Location of the default credential file. "~/.sauce-ondemand"
     *
     * <p>
     * This common convention allows all the tools that interact with Sauce OnDemand REST API
     * to use the single credential, thereby simplifying the user configuration.
     */
    public static File getDefaultCredentialFile() {
        return new File(new File(System.getProperty("user.home")),".sauce-ondemand");
    }
}