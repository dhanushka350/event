package com.akvasoft.events.config;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.stereotype.Component;

@Component
public class DriverInitializer {


    public FirefoxDriver getFirefoxDriver() {
        System.setProperty("webdriver.gecko.driver", "/var/lib/tomcat8/geckodriver");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true");
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(false);
        return new FirefoxDriver(options);
    }

    public ChromeDriver getChromeDriver() {
        System.setProperty("webdriver.chrome.driver", "/var/lib/tomcat8/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        return new ChromeDriver(options);
    }
}
