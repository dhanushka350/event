package com.akvasoft.events.config;

import org.apache.commons.net.ftp.FTPClient;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class FileUpload {
    private static final Logger LOGGER = Logger.getLogger(FileUpload.class.getName());

    public static void uploadToWhatsonyarravalley(FirefoxDriver driver, String file) throws InterruptedException {

        while (true) {
            try {
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
                browse.sendKeys(file);
                WebElement submit = driver.findElementByXPath("//*[@id=\"submit\"]");
                submit.click();

                driver.switchTo().alert().accept();
                Thread.sleep(60000);
                LOGGER.info("FILE UPLOAD : END");
                break;
            } catch (Exception e) {
                try {
                    driver.get("https://whatsonyarravalley.com.au/wp-admin/admin.php?page=bulk_upload");
                    WebElement browse = driver.findElementByXPath("//*[@id=\"csv_import\"]");
                    browse.sendKeys("/var/lib/tomcat8/EVENTS.csv");
                    WebElement submit = driver.findElementByXPath("//*[@id=\"submit\"]");
                    submit.click();

                    driver.switchTo().alert().accept();
                    Thread.sleep(60000);
                    LOGGER.info("FILE UPLOAD : END");
                    break;
                } catch (Exception c) {
                    continue;
                }

            }

        }
    }

    public void uploadImages() {
        FTPClient client = new FTPClient();
        FileInputStream inputStream = null;
        List<String> savedImages = getSavedImages();
        int count = 1;
        try {
            LOGGER.info("LOGGING TO FTP SERVER");
            client.connect("ftp.pinkchilli.com.au");
            client.login("eminda@pinkchilli.com.au", "Eminda123");

            for (String image : savedImages) {
                try {
                    inputStream = new FileInputStream("/asset/bulk/testImage1.jpeg");
                    client.setFileType(FTPClient.BINARY_FILE_TYPE);
                    client.storeFile("up2", inputStream);
                    count++;
                    LOGGER.info("UPLOADING IMAGE :- " + image);
                } catch (Exception e) {
                    LOGGER.warning("FAILED TO UPLOAD :- " + image);
                    e.printStackTrace();
                    continue;
                }
                break;
            }
            client.logout();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {

                if (inputStream != null) {
                    inputStream.close();
                }
                client.disconnect();
                LOGGER.warning("DISCONNECTED");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("");

    }

    private List<String> getSavedImages() {
        List<String> list = new ArrayList<>();
        final File folder = new File("/asset/bulk/");
        for (final File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                list.add(file.getName());
                LOGGER.info("READING IMAGE :- " + file.getName());
            }
        }
        return list;
    }
}
