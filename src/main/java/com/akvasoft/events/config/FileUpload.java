package com.akvasoft.events.config;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class FileUpload {
    private static final Logger LOGGER = Logger.getLogger(FileUpload.class.getName());

    public static void uploadToWhatsonyarravalley(FirefoxDriver driver) {
        LOGGER.info("FILE UPLOAD : START");

        driver.get("https://whatsonyarravalley.com.au/wp-admin/");
        WebElement email = driver.findElementByXPath("//*[@id=\"user_login\"]");
        WebElement password = driver.findElementByXPath("//*[@id=\"user_pass\"]");
        WebElement login = driver.findElementByXPath("//*[@id=\"wp-submit\"]");

        email.sendKeys("Wo");
        password.sendKeys("Woyv@100");
        login.click();

        driver.get("https://whatsonyarravalley.com.au/wp-admin/admin.php?page=bulk_upload");
        WebElement browse = driver.findElementByXPath("//*[@id=\"csv_import\"]");
//        browse.sendKeys("/home/dhanushka/Downloads/GoogleImportsI.csv");
        browse.sendKeys("/var/lib/tomcat8/EVENTS.xlsx");
        WebElement submit = driver.findElementByXPath("//*[@id=\"submit\"]");
        submit.click();

        driver.switchTo().alert().accept();
        LOGGER.info("FILE UPLOAD : END");
    }
}
