package com.webgenerals.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * LocalProperties
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class LocalProperties {

    private Properties properties = new Properties();

    public LocalProperties() {
        try {
            InputStream input = this.getClass().getClassLoader().getResourceAsStream("application-local.properties");
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getConsumerKey() {
        return (String) properties.get("app.twitter.consumer.key");
    }

    public String getConsumerSecret() {
        return (String) properties.get("app.twitter.consumer.secret");
    }

    public String getAccessToken() {
        return (String) properties.get("app.twitter.access.token");
    }

    public String getAccessSecret() {
        return (String) properties.get("app.twitter.access.secret");
    }
}
