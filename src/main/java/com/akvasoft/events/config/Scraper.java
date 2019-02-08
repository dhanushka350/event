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
//        latlongDriver.get("https://www.latlong.net");
        latlongDriver.get("https://gps-coordinates.org/coordinate-converter.php");

        List<City> allCities = eventService.getAllCities();

        searchGoogle(driver, allCities);
    }

    private void searchGoogle(FirefoxDriver driver, List<City> cityList) throws InterruptedException, IOException {
        List<String> eventList = new ArrayList<>();
        List<ExcelData> excelList = new ArrayList<>();
        for (City city : cityList) {
            driver.get("https://www.google.com/");
            WebElement input = driver.findElementByXPath("/html/body/div/div[3]/form/div[2]/div/div[1]/div/div[1]/input");
            input.sendKeys("events " + city.getCity_Name());
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


                excelList.add(saveEvent(iuf4Uc, driver, website, excelList, city));
                Thread.sleep(2000);
            }
            eventService.createExcelFile(excelList);
            break;
        }

    }

    private ExcelData saveEvent(WebElement iuf4Uc, FirefoxDriver driver, String website, List<ExcelData> eventList, City city) throws InterruptedException {

        String name = iuf4Uc.findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String date = iuf4Uc.findElements(By.xpath("./*")).get(1).getAttribute("innerText");
        String location = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(0).getAttribute("innerText");
        String organizer = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(0).findElement(By.tagName("a")).getAttribute("href");
        String address = iuf4Uc.findElements(By.xpath("./*")).get(2).findElements(By.xpath("./*")).get(1).getAttribute("innerText");
        String latlong = getLatitude(address);
        String image = saveImage(driver, iuf4Uc.findElements(By.xpath("./*")).get(0).getAttribute("innerText")
                .replace(" ", "_")
                .replace("-", "_"));
        String description = getDescription(website);

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
        event.setCategory("event");
        event.setDate(formattedDate);
        event.setImage(image);
        event.setLatitude(latlong.split("@")[0]);
        event.setLongitude(latlong.split("@")[1]);
        event.setDay(fullDate[0]);
        event.setLocation(location);
        event.setName(name);
        event.setTime(time);
        event.setWebsite(website);
        Organizer organizer1 = getOrganizer(organizer, driver, website);
        eventService.saveEvent(event);

        ExcelData data = new ExcelData();
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
        data.setTemplatic_post_name(name);
        data.setTemplatic_post_status("publish");
        data.setTemplatic_post_title(name);
        data.setTemplatic_post_type("event");
        data.setZones_id(city.getZones_Id());
        return data;

    }

    private String getDescription(String website) {

        if (website.contains("eventbrite.com.au")) {
            return "--";
        } else if (website.contains("permaculturevictoria.org.au")) {
            return "--";
        } else if (website.contains("meetup.com")) {
            return "--";
        } else if (website.contains("activeapril.vic.gov.au")) {
            return "--";
        } else if (website.contains("kinglakeranges.com.au")) {
            return "--";
        } else if (website.contains("evensi.com")) {
            return "--";
        } else if (website.contains("yourlibrary.com.au")) {
            return "--";
        } else if (website.contains("onlymelbourne.com.au")) {
            return "--";
        } else if (website.contains("maroondah.vic.gov.au")) {
            return "--";
        } else if (website.contains("gawler.org")) {
            return "--";
        } else if (website.contains("bandsintown.com")) {
            return "--";
        } else if (website.contains("zumba.com")) {
            return "--";
        } else if (website.contains("fieldandgame.com.au")) {
            return "--";
        } else if (website.contains("eventful.com")) {
            return "--";
        } else if (website.contains("songkick.com")) {
            return "--";
        } else if (website.contains("facebook.com")) {
            return "--";
        } else if (website.contains("vicparks.com.au")) {
            return "--";
        } else if (website.contains("thehomehotel.net.au")) {
            return "--";
        } else if (website.contains("whittleseacountrymusicfestival.com.au")) {
            return "--";
        } else if (website.contains("boroondara.vic.gov.au")) {
            return "--";
        } else if (website.contains("nunawadingswimmingclub.com")) {
            return "--";
        } else if (website.contains("avocapark.com.au")) {
            return "--";
        } else if (website.contains("fredstinyhouses.com.au")) {
            return "--";
        } else {
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
                System.err.println("found===========================" + innerHTML.split("Phone")[1]);
                phone = innerHTML.split("Phone")[1];
                break;
            }

        }

        organizer1.setOrganizer_mobile(phone);
        organizer1.setOrganizer_name(organizer_name);
        organizer1.setOrganizer_website(website);

        System.out.println(organizer_name + " <<<<<<------------------ organizer_name");
        System.out.println(phone + " <<<<<<------------------ organizer_phone");
        System.out.println(website + " <<<<<<------------------ organizer_website");
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
//            e.printStackTrace();
            System.err.println("ERROR IN IMAGE SAVE METHOD. MALFORMED CATCH CLAUSE");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        } catch (IOException e) {
//            e.printStackTrace();
            System.err.println("ERROR IN IMAGE SAVE METHOD. IO CATCH CLAUSE");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        } catch (IllegalArgumentException t) {
            System.err.println("ERROR IN IMAGE SAVE METHOD. ILLEGAL ARGUMENT EXCEPTION");
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/image-not-found.png";
        }
    }

    private String getLatitude(String address) throws InterruptedException {

        String latitude = "";
        String longitude = "";
        int count = 0;

        while (true) {
            System.out.println("ADDRESS  " + address + "******************************************");
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


            WebElement place = latlongDriver.findElementByXPath("//*[@id=\"address\"]");
            place.clear();
            place.sendKeys(address);
            Thread.sleep(1000);
            latlongDriver.findElementByXPath("//*[@id=\"btnGetGpsCoordinates\"]").click();
            latlongDriver.findElementByXPath("//*[@id=\"btnGetGpsCoordinates\"]").click();
            Thread.sleep(5000);


            latitude = latlongDriver.findElementByXPath("//*[@id=\"latitude\"]").getAttribute("value");
            longitude = latlongDriver.findElementByXPath("//*[@id=\"longitude\"]").getAttribute("value");

            System.out.println(latitude + " ------ " + longitude);

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
