package com.akvasoft.events.config;

import com.akvasoft.events.modal.Event;
import com.akvasoft.events.service.EventService;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class Scraper implements InitializingBean {

    @Autowired
    private EventService eventService;

    FirefoxDriver latlongDriver;

    @Override
    public void afterPropertiesSet() throws Exception {
        FirefoxDriver driver = new DriverInitializer().getFirefoxDriver();
        latlongDriver = new DriverInitializer().getFirefoxDriver();
        latlongDriver.get("https://www.latlong.net");

        List<String> cityList = new ArrayList<>();
        cityList.add("london");

        searchGoogle(driver, cityList);
    }

    private void searchGoogle(FirefoxDriver driver, List<String> cityList) throws InterruptedException {
        List<String> eventList = new ArrayList<>();
        for (String city : cityList) {
            driver.get("https://www.google.com/");
            WebElement input = driver.findElementByXPath("/html/body/div/div[3]/form/div[2]/div/div[1]/div/div[1]/input");
            input.sendKeys("events " + city);
            input.sendKeys(Keys.ENTER);

            Thread.sleep(3000);
            WebElement mainEventDiv = driver.findElementByXPath("/html/body/div[6]/div[3]/div[7]/div[1]/div/div/div/div/div/div[2]/div/g-scrolling-carousel/div/div");
            for (WebElement events : mainEventDiv.findElements(By.xpath("./*"))) {
                for (WebElement div : events.findElement(By.tagName("div")).findElements(By.xpath("./*"))) {
                    eventList.add(div.findElement(By.tagName("a")).getAttribute("href"));
                }

            }
            System.out.println("* == FOUND " + eventList.size() + " EVENTS == *");


            for (String event : eventList) {
                driver.get(event);
                WebElement web2 = null;
                WebElement web = null;

                WebElement mainInfoDiv = driver.findElementByXPath("//*[@id=\"rso\"]");
                WebElement div = mainInfoDiv.findElements(By.xpath("./*")).get(0).findElement(By.tagName("div"));
                web = mainInfoDiv.findElements(By.xpath("./*")).get(1).findElement(By.tagName("div"));

                // main info div array size can be changed.
                try {
                    // element index 2 can be a span. there is no div inside it
                    try {
                        web2 = mainInfoDiv.findElements(By.xpath("./*")).get(2).findElement(By.tagName("div"));
                    } catch (NoSuchElementException r) {
                        web2 = mainInfoDiv.findElements(By.xpath("./*")).get(3).findElement(By.tagName("div"));
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("ARRAY LENGTH LESS THAN 2.");
                }

                String infoClass = div.getAttribute("class");
                if (!infoClass.equalsIgnoreCase("g mnr-c g-blk")) {
                    System.err.println("g mnr-c g-blk class not found.");
                    System.err.println("SKIPPING");
                    continue;
                }

                WebElement data = null;
                try {
                    data = div.findElement(By.className("ifM9O")).findElements(By.xpath("./*")).get(1)
                            .findElements(By.xpath("./*")).get(1);
                } catch (IndexOutOfBoundsException e) {
                    data = div.findElement(By.className("ifM9O")).findElements(By.xpath("./*")).get(1)
                            .findElements(By.xpath("./*")).get(0);
                }

                WebElement iuf4Uc = data.findElement(By.className("IUF4Uc"));
                String website = "";
                try {
                    website = web.findElements(By.xpath("./*")).get(0).findElement(By.tagName("div")).findElement(By.tagName("div"))
                            .findElement(By.tagName("a")).getAttribute("href");
                } catch (NoSuchElementException e) {
                    website = web2.findElements(By.xpath("./*")).get(0).findElement(By.tagName("div")).findElement(By.tagName("div"))
                            .findElement(By.tagName("a")).getAttribute("href");
                }

                saveEvent(iuf4Uc, driver, website);
                Thread.sleep(2000);
            }


            break;
        }

    }

    private void saveEvent(WebElement iuf4Uc, FirefoxDriver driver, String website) throws InterruptedException {
        String name = iuf4Uc.findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String date = iuf4Uc.findElements(By.xpath("./*")).get(1).getAttribute("innerText");
        String location = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String address = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(1).getAttribute("innerText");
        String latlong = getLatitude(address);
        String image = saveImage(driver, iuf4Uc.findElements(By.xpath("./*")).get(0).getAttribute("innerText")
                .replace(" ", "_")
                .replace("-", "_"));

        String[] fullDate = date.split(",");
        String day = fullDate[0];
        String formattedDate = fixDateFormat(fullDate[1], fullDate[2]);
        String time = fullDate[fullDate.length - 1];
        if (time.contains("AM") || time.contains("PM")) {
            System.out.println("TIME IS SET");
        } else {
            time = "-";
            System.out.println("TIME NOT FOUND");
        }

        Event event = new Event();
        event.setAddress(address);
        event.setCategory("");
        event.setDate(formattedDate);
        event.setImage(image);
        event.setLatitude(latlong.split("-")[0]);
        event.setLongitude(latlong.split("-")[1]);
        event.setDay(fullDate[0]);
        event.setLocation(location);
        event.setName(name);
        event.setTime(time);
        event.setWebsite(website);
        eventService.saveEvent(event);

        System.err.println("===========================================================");
        System.err.println("===========================SAVED===========================");
        System.err.println("===========================================================");


        System.out.println("NAME      - " + name);
        System.out.println("DATE      - " + formattedDate);
        System.out.println("LOCATION  - " + location);
        System.out.println("ADDRESS   - " + address);
        System.out.println("IMAGE     - " + image);
        System.out.println("LAT LONG  - " + latlong);
        System.out.println("DAY       - " + day);
        System.out.println("TIME      - " + fullDate[fullDate.length - 1]);
        System.out.println("WEBSITE   - " + website);


    }

    private String fixDateFormat(String fullMonth, String year) {


        System.out.println("===========================================================");
        System.out.println("=======================FIXING DATE=========================");
        System.out.println("===========================================================");
        System.out.println("=======================" + fullMonth + "=========================");

        String month = fullMonth.split(" ")[1];
        String day = fullMonth.split(" ")[2];
        month = month.trim();


        if (month.equalsIgnoreCase("January")) {
            month = "01";
        } else if (month.equalsIgnoreCase("February")) {
            month = "02";
        } else if (month.equalsIgnoreCase("March")) {
            month = "03";
        } else if (month.equalsIgnoreCase("April")) {
            month = "04";
        } else if (month.equalsIgnoreCase("May")) {
            month = "05";
        } else if (month.equalsIgnoreCase("June")) {
            month = "06";
        } else if (month.equalsIgnoreCase("July")) {
            month = "07";
        } else if (month.equalsIgnoreCase("August")) {
            month = "08";
        } else if (month.equalsIgnoreCase("September")) {
            month = "09";
        } else if (month.equalsIgnoreCase("October")) {
            month = "10";
        } else if (month.equalsIgnoreCase("November")) {
            month = "11";
        } else if (month.equalsIgnoreCase("December")) {
            month = "12";
        }

        return month + "/" + day + "/" + year.trim();

    }

    private String saveImage(FirefoxDriver driver, String event) throws InterruptedException {
        for (WebElement nav : driver.findElementByXPath("//*[@id=\"hdtb-msb-vis\"]").findElements(By.xpath("./*"))) {
            try {
                if (nav.findElement(By.tagName("a")).getAttribute("innerText").equalsIgnoreCase("Images")) {
                    driver.get(nav.findElement(By.tagName("a")).getAttribute("href"));
                    break;
                }
            } catch (NoSuchElementException e) {
                System.err.println("EXCEPTION  - Method save image. clicking on navigation for images. line 99");
            }
        }

        driver.findElementByXPath("//*[@id=\"rg_s\"]").findElements(By.xpath("./*")).get(0)
                .findElements(By.xpath("./*")).get(0).click();

        Thread.sleep(1000);
        WebElement bigImage = driver.findElementByXPath("//*[@id=\"irc_bg\"]");
        String src = bigImage.findElement(By.id("irc-cl")).findElement(By.id("irc_cc"))
                .findElements(By.xpath("./*")).get(1).findElements(By.xpath("./*")).get(0)
                .findElement(By.className("irc_mic")).findElements(By.xpath("./*")).get(0)
                .findElement(By.tagName("a")).findElement(By.tagName("img")).getAttribute("src");
        try {
            URL imageUrl = new URL(src);
            BufferedImage saveImage = ImageIO.read(imageUrl);
            ImageIO.write(saveImage, "jpg", new File("/home/dhanushka/Desktop/ooo/" + event + ".jpg"));
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/" + event + ".jpg";

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        } catch (IOException e) {
            e.printStackTrace();
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        }
    }

    private String getLatitude(String address) throws InterruptedException {

        WebElement place = latlongDriver.findElementByXPath("//*[@id=\"place\"]");

        place.clear();
        place.sendKeys(address);
        Thread.sleep(1000);
        place.sendKeys(Keys.ENTER);
        Thread.sleep(5000);

        String latitude = latlongDriver.findElementByXPath("//*[@id=\"lat\"]").getAttribute("value");
        String longitude = latlongDriver.findElementByXPath("//*[@id=\"lng\"]").getAttribute("value");
        latlongDriver.findElementByXPath("//*[@id=\"lat\"]").clear();
        latlongDriver.findElementByXPath("//*[@id=\"lng\"]").clear();
        return latitude + "-" + longitude;
    }

}
