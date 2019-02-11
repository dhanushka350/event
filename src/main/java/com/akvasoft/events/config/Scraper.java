package com.akvasoft.events.config;

import com.akvasoft.events.dto.ExcelData;
import com.akvasoft.events.dto.Organizer;
import com.akvasoft.events.modal.City;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class Scraper implements InitializingBean {

    @Autowired
    private EventService eventService;

    FirefoxDriver latlongDriver;
    private static final Logger LOGGER = Logger.getLogger(Scraper.class.getName());

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("INITIALIZING DRIVERS");

        FirefoxDriver driver = new DriverInitializer().getFirefoxDriver();
        latlongDriver = new DriverInitializer().getFirefoxDriver();
        latlongDriver.get("https://gps-coordinates.org/coordinate-converter.php");
        List<City> allCities = eventService.getAllCities();

        LOGGER.info("FOUND " + allCities.size() + " CITIES");
        searchGoogle(driver, allCities);
        LOGGER.info("SCRAPE FINISHED.");
    }

    private void searchGoogle(FirefoxDriver driver, List<City> cityList) throws InterruptedException, IOException {
        List<String> eventList = new ArrayList<>();
        List<ExcelData> excelList = new ArrayList<>();
        int cityCount = 1;
        for (City city : cityList) {
            driver.get("https://www.google.com/");
            WebElement input = driver.findElementByXPath("/html/body/div/div[3]/form/div[2]/div/div[1]/div/div[1]/input");
            input.sendKeys("events " + city.getCity_Name());
            input.sendKeys(Keys.ENTER);

            Thread.sleep(3000);
            WebElement mainEventDiv = null;
            try {
                mainEventDiv = driver.findElementByXPath("/html/body/div[6]/div[3]/div[7]/div[1]/div/div/div/div/div/div[2]/div/g-scrolling-carousel/div/div");
            } catch (NoSuchElementException t) {
                LOGGER.warning("LINE 65 | SKIPPING CITY : " + city.getCity_Name() + " CAN NOT FIND EVENTS IN GOOGLE.");
                continue;
            }
            for (WebElement events : mainEventDiv.findElements(By.xpath("./*"))) {
                for (WebElement div : events.findElement(By.tagName("div")).findElements(By.xpath("./*"))) {
                    eventList.add(div.findElement(By.tagName("a")).getAttribute("href"));
                }

            }

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
                    LOGGER.warning("LINE 87 | ARRAY LENGTH LESS THAN 2. Can cause ArrayIndexOutOfBoundsException");
                }


                String infoClass = div.getAttribute("class");
                if (!infoClass.equalsIgnoreCase("g mnr-c g-blk")) {
                    LOGGER.warning("LINE 87 | EXPECTED CLASS NOT FOUND. SKIPPING, Can cause NoSuchElementException");
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


                saveEvent(iuf4Uc, driver, website, excelList, city);
                Thread.sleep(2000);
            }
            LOGGER.info("SCRAPED CITY COUNT : " + cityCount + " , CURRENT CITY : " + city.getCity_Name());
            if (cityCount == 5) {
                break;
            }
            cityCount++;

        }
        eventService.createExcelFile(excelList);
    }

    private void saveEvent(WebElement iuf4Uc, FirefoxDriver driver, String website, List<ExcelData> eventList, City city) throws InterruptedException {

        String name = iuf4Uc.findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String date = iuf4Uc.findElements(By.xpath("./*")).get(1).getAttribute("innerText");
        String location = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String organizer = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(0).findElement(By.tagName("a")).getAttribute("href");
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

        } else {
            time = "-";
        }


//        Event event = new Event();
//        event.setAddress(address);
//        event.setCategory("event");
//        event.setDate(formattedDate);
//        event.setImage(image);
//        event.setLatitude(latlong.split("@")[0]);
//        event.setLongitude(latlong.split("@")[1]);
//        event.setDay(fullDate[0]);
//        event.setLocation(location);
//        event.setName(name);
//        event.setTime(time);
//        event.setWebsite(website);
//        eventService.saveEvent(event);

        Organizer organizer1 = getOrganizer(organizer, driver, website);
        String description = getDescription(website, driver);

        Event data = new Event();
        data.setAddress(address);
        data.setAlive_days("9");
        data.setCountry_id(city.getCountry_Id());
        data.setEnd_date("-");
        data.setGeo_latitude(latlong.split("@")[0]);
        data.setGeo_longitude(latlong.split("@")[1]);
        data.setMap_view(city.getMap_Type());
        data.setOrganizer_mobile(organizer1.getOrganizer_mobile());
        data.setOrganizer_name(organizer1.getOrganizer_name());
        data.setOrganizer_website(organizer1.getOrganizer_website());
        data.setPackage_id("55");
        data.setPost_city_id(city.getId() + "");
        data.setSt_date(formattedDate);
        data.setSt_time(time);
        data.setTemplatic_comment_status("open");
        data.setTemplatic_img(image);
        data.setTemplatic_ping_status("publish");
        data.setTemplatic_post_author("1");
        data.setTemplatic_post_category("events");
        data.setTemplatic_post_content("?");
        data.setTemplatic_post_date(formattedDate + " " + time);
        data.setTemplaticPostName(name);
        data.setTemplatic_post_status("publish");
        data.setTemplatic_post_title(name);
        data.setTemplatic_post_type("event");
        data.setZones_id(city.getZones_Id());

    }

    private String getDescription(String website, FirefoxDriver driver) {
        LOGGER.info("SEARCHING SITE FOR DESCRIPTION. - " + website);

        if (website.contains("eventbrite.com.au")) {
            LOGGER.info("GETTING FROM : - eventbrite.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/main/div[1]/div[2]/div/section[1]/div[1]/div/div/div[1]/div[1]/div[2]/div/div[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("permaculturevictoria.org.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - permaculturevictoria.org.au");
            return "--";
        } else if (website.contains("meetup.com")) {
            LOGGER.info("GETTING FROM : - meetup.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div[2]/div/div/div/div[2]/div[3]/main/div/div[2]/div/div[2]/section[1]/div/div")
                        .getAttribute("innerText");
                System.err.println(desc);

            } catch (NoSuchElementException e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div/div/div[2]/div/div/div/div[2]/div[3]/main/div/div[3]/div/div[2]/section[1]/div/div/p")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
//                    v.printStackTrace();
                }
            }
            return desc;
        } else if (website.contains("activeapril.vic.gov.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - activeapril.vic.gov.au");
            return "--";
        } else if (website.contains("kinglakeranges.com.au")) {
            LOGGER.info("GETTING FROM : - kinglakeranges.com.au");
            driver.get(website);
            String desc = "--";
//            try {
//                desc = driver.findElementByXPath("/html/body/div[2]/div[2]/div/h5[2]").getAttribute("innerText");
//                if (desc.length() < 2) {
//                    desc = driver.findElementByXPath("/html/body/div[1]/div[3]/div[1]/div/div[2]/div/div/div[2]/div/div/div[2]/div[3]/div/div[1]/div/div/div[2]/div/div/div/span")
//                            .getAttribute("innerText");
//                }
//                System.err.println(desc);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            return desc;
        } else if (website.contains("evensi.com")) {
            LOGGER.info("GETTING FROM : - evensi.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[3]/div[2]/article/div[2]/div[2]/div[4]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("yourlibrary.com.au")) {
            LOGGER.info("GETTING FROM : - yourlibrary.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[2]/div[2]/div/h5[2]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("onlymelbourne.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - onlymelbourne.com.au");
            return "--";
        } else if (website.contains("maroondah.vic.gov.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - maroondah.vic.gov.au");
            return "--";
        } else if (website.contains("gawler.org")) {
            LOGGER.info("GETTING FROM : - gawler.org");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div/div/section/main/div[1]/article/div[1]/table/tbody/tr[3]/td[2]/span")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("bandsintown.com")) {
            LOGGER.info("GETTING FROM : - bandsintown.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div/div/div[2]/div[2]/div[1]/div[4]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("zumba.com")) {
            LOGGER.warning("NEED UPDATE | FOUND : - zumba.com");
            return "--";
        } else if (website.contains("fieldandgame.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - fieldandgame.com.au");
            return "--";
        } else if (website.contains("eventful.com")) {
            LOGGER.info("GETTING FROM : - eventful.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div[4]/div[2]/div[1]/div[4]/div[1]/p[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return desc;
        } else if (website.contains("songkick.com")) {
            LOGGER.warning("NEED UPDATE | FOUND : - songkick.com");
            driver.get(website);
            String desc = "--";

            return desc;
        } else if (website.contains("facebook.com")) {
            LOGGER.info("GETTING FROM : - facebook.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div[3]/div[1]/div/div[2]/div/div/div[2]/div/div/div[2]/div[3]/div/div[2]/div/div/div[2]/div/div/div")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[1]/div[3]/div[1]/div/div[2]/div/div/div[2]/div/div/div[2]/div[3]/div/div[2]/div/div/div[2]/div/div/div/span")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
//                    v.printStackTrace();
                }
            }
            return desc;
        } else if (website.contains("vicparks.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - vicparks.com.au");
            return "--";
        } else if (website.contains("thehomehotel.net.au")) {
            LOGGER.warning("GETTING FROM : - thehomehotel.net.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div/div[2]/div/article/div/div/div/div[1]/div/div/div/div/div/div/div/div/div[3]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div/div[2]/div/article/div/div/div/div[1]/div/div/div/div/div/div/div/div/div[3]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {

                }
            }
            return desc;
        } else if (website.contains("whittleseacountrymusicfestival.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - whittleseacountrymusicfestival.com.au");
            return "--";
        } else if (website.contains("boroondara.vic.gov.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - boroondara.vic.gov.au");
            return "--";
        } else if (website.contains("nunawadingswimmingclub.com")) {
            LOGGER.warning("NEED UPDATE | FOUND : - nunawadingswimmingclub.com");
            return "--";
        } else if (website.contains("avocapark.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - avocapark.com.au");
            return "--";
        } else if (website.contains("fredstinyhouses.com.au")) {
            LOGGER.warning("NEED UPDATE | FOUND : - fredstinyhouses.com.au");
            return "--";
        } else {
            LOGGER.warning("SITE NOT IN EXPECTED LIST");
            return "--";
        }

    }

    private Organizer getOrganizer(String organizer, FirefoxDriver driver, String website) {
        driver.get(organizer);
        Organizer organizer1 = new Organizer();
        WebElement mainDiv = driver.findElementByXPath("//*[@id=\"rhs_block\"]");
        String organizer_name = mainDiv.findElement(By.tagName("div")).findElements(By.xpath("./*")).get(0).findElement(By.tagName("div"))//xpdopen
                .findElements(By.xpath("./*")).get(0).findElements(By.xpath("./*")).get(1)                              //<div data-ved="2ahUKEwjs28OImqzgAhXLRY8KHV5DCFcQ_xd6BAgXEAI">
                .findElements(By.xpath("./*")).get(1).findElement(By.tagName("div")).findElements(By.xpath("./*")).get(1)
                .findElements(By.xpath("./*")).get(0).findElement(By.tagName("div")).findElement(By.tagName("div"))
                .findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String phone = "";
        for (WebElement element : mainDiv.findElement(By.tagName("div")).findElements(By.xpath("./*")).get(0).findElement(By.tagName("div"))//xpdopen
                .findElements(By.xpath("./*")).get(0).findElements(By.xpath("./*")).get(1)                              //<div data-ved="2ahUKEwjs28OImqzgAhXLRY8KHV5DCFcQ_xd6BAgXEAI">
                .findElements(By.xpath("./*"))) {

            String innerHTML = element.getAttribute("innerText");
            if (innerHTML.contains("Phone")) {
                phone = innerHTML.split("Phone")[1];
                break;
            }

        }

        organizer1.setOrganizer_mobile(phone);
        organizer1.setOrganizer_name(organizer_name);
        organizer1.setOrganizer_website(website);

        return organizer1;
    }

    private String fixDateFormat(String fullMonth, String year) {

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

        return month + "-" + day + "-" + year.trim();

    }

    private String saveImage(FirefoxDriver driver, String event) throws InterruptedException {
        for (WebElement nav : driver.findElementByXPath("//*[@id=\"hdtb-msb-vis\"]").findElements(By.xpath("./*"))) {
            try {
                if (nav.findElement(By.tagName("a")).getAttribute("innerText").equalsIgnoreCase("Images")) {
                    driver.get(nav.findElement(By.tagName("a")).getAttribute("href"));
                    break;
                }
            } catch (NoSuchElementException e) {
                LOGGER.warning("NSE EXCEPTION  - Method save image. clicking on navigation for images. line 419");
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
        event = event.replace("-", "_");
        LOGGER.info("SAVING IMAGE URL " + src);
        try {
            URL imageUrl = new URL(src);
            BufferedImage saveImage = ImageIO.read(imageUrl);
            BufferedImage newBufferedImage = new BufferedImage(saveImage.getWidth(),
                    saveImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(saveImage, 0, 0, Color.WHITE, null);
            ImageIO.write(newBufferedImage, "jpg", new File("/home/dhanushka/Desktop/ooo/" + event + ".jpg"));
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/" + event + ".jpg";

        } catch (MalformedURLException e) {
            e.printStackTrace();
            LOGGER.warning("ERROR IN IMAGE SAVE METHOD. MALFORMED CATCH CLAUSE | LINE 440");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.warning("ERROR IN IMAGE SAVE METHOD. IO CATCH CLAUSE | LINE 444");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        } catch (IllegalArgumentException t) {
            LOGGER.warning("ERROR IN IMAGE SAVE METHOD. ILLEGAL ARGUMENT EXCEPTION | LINE 447");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        }
    }

    private String getLatitude(String address) throws InterruptedException {

        String latitude = "";
        String longitude = "";
        int count = 0;

        while (true) {
            // https://www.latlong.net

//        WebElement place = latlongDriver.findElementByXPath("//*[@id=\"place\"]");
//
//        place.clear();
//        place.sendKeys(address);
//        Thread.sleep(1000);
//        place.sendKeys(Keys.ENTER);
//        Thread.sleep(5000);
//
//        String latitude = latlongDriver.findElementByXPath("//*[@id=\"lat\"]").getAttribute("value");
//        String longitude = latlongDriver.findElementByXPath("//*[@id=\"lng\"]").getAttribute("value");
//        latlongDriver.findElementByXPath("//*[@id=\"lat\"]").clear();
//        latlongDriver.findElementByXPath("//*[@id=\"lng\"]").clear();

            WebElement place = null;
            try {
                place = latlongDriver.findElementByXPath("//*[@id=\"address\"]");
            } catch (NoSuchElementException nosez) {
                LOGGER.warning("EXCEPTION IN COORDINATES METHOD | SITE URL REDIRECTED.");
                latlongDriver.get("https://gps-coordinates.org/coordinate-converter.php");
                place = latlongDriver.findElementByXPath("//*[@id=\"address\"]");
            }

            place.clear();
            place.sendKeys(address);
            Thread.sleep(1000);
            latlongDriver.findElementByXPath("//*[@id=\"btnGetGpsCoordinates\"]").click();
            latlongDriver.findElementByXPath("//*[@id=\"btnGetGpsCoordinates\"]").click();
            Thread.sleep(5000);


            latitude = latlongDriver.findElementByXPath("//*[@id=\"latitude\"]").getAttribute("value");
            longitude = latlongDriver.findElementByXPath("//*[@id=\"longitude\"]").getAttribute("value");


            latlongDriver.findElementByXPath("//*[@id=\"latitude\"]").clear();
            latlongDriver.findElementByXPath("//*[@id=\"longitude\"]").clear();
            latlongDriver.findElementByXPath("//*[@id=\"address\"]").clear();


            if (latitude.length() > 2 && longitude.length() > 2) {
                break;
            } else if (count > 5) {
                latitude = "not found";
                longitude = "not found";
                break;
            }
            latlongDriver.get("https://gps-coordinates.org/");
            Thread.sleep(2000);
            count++;
        }
        return latitude + "@" + longitude;
    }
}
