package com.akvasoft.events.config;

import com.akvasoft.events.dto.ExcelData;
import com.akvasoft.events.dto.Organizer;
import com.akvasoft.events.modal.City;
import com.akvasoft.events.modal.Event;
import com.akvasoft.events.service.EventService;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

@Component
public class Scraper implements InitializingBean {

    @Autowired
    private EventService eventService;


    FirefoxDriver latlongDriver;
    FirefoxDriver driver;
    FileUpload fileUpload;
    private static final Logger LOGGER = Logger.getLogger(Scraper.class.getName());

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("INITIALIZING DRIVERS");

        new Thread(() -> {
            while (true) {
                System.err.println("NEW ROUND");
                System.gc();
                eventService.resumeCityStatus();
                eventService.resetCities();
                try {
                    searchGoogle();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }).start();
    }


    private void searchGoogle() throws Exception, SessionNotCreatedException {
        try {
            List<String> eventList = new ArrayList<>();
            int cityCount = 1;

            while (true) {
                City city = findAvailableCity();
                System.out.println("==========================" + city.getCity_Name());
                if (city == null) {
                    break;
                }

                latlongDriver = new DriverInitializer().getFirefoxDriver();
                latlongDriver.get("https://gps-coordinates.org/coordinate-converter.php");
                driver = new DriverInitializer().getFirefoxDriver();

                try {
                    driver.get("https://www.google.com/");

                } catch (SessionNotCreatedException e) {
                    try {
                        driver.close();
                        latlongDriver.close();
                    } catch (Exception t) {
                    }
                    driver = new DriverInitializer().getFirefoxDriver();
                    latlongDriver = new DriverInitializer().getFirefoxDriver();
                    latlongDriver.get("https://gps-coordinates.org/coordinate-converter.php");
                }
                WebElement input = driver.findElementByXPath("/html/body/div/div[3]/form/div[2]/div/div[1]/div/div[1]/input");
                input.sendKeys("events " + city.getCity_Name() + " australia");
                input.sendKeys(Keys.ENTER);

                Thread.sleep(3000);
                WebElement mainEventDiv = null;
                try {
                    mainEventDiv = driver.findElementByXPath("/html/body/div[6]/div[3]/div[7]/div[1]/div/div/div/div/div/div[2]/div/g-scrolling-carousel/div/div");
                } catch (NoSuchElementException t) {
                    LOGGER.warning("LINE 110 | SKIPPING CITY : " + city.getCity_Name() + " CAN NOT FIND EVENTS IN GOOGLE.");
                    eventService.updateCityStatus(city, "SKIPPED");
                    continue;
                }
                for (WebElement events : mainEventDiv.findElements(By.xpath("./*"))) {
                    for (WebElement div : events.findElement(By.tagName("div")).findElements(By.xpath("./*"))) {
                        eventList.add(div.findElement(By.tagName("a")).getAttribute("href"));
                    }

                }


                for (String event : eventList) {
                    try {
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
                            LOGGER.warning("LINE 139 | ARRAY LENGTH LESS THAN 2. Can cause ArrayIndexOutOfBoundsException");
                        }


                        String infoClass = div.getAttribute("class");
                        if (!infoClass.equalsIgnoreCase("g mnr-c g-blk")) {
                            LOGGER.warning("LINE 145 | EXPECTED CLASS NOT FOUND. SKIPPING, Can cause NoSuchElementException");
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


                        saveEvent(iuf4Uc, driver, website, city);
                        Thread.sleep(5000);

                    } catch (Exception f) {
                        LOGGER.info("STOPPED.." + f.getMessage());
                        LOGGER.info("SKIPPING EVENT");
                        continue;
                    }

                }

                driver.close();
                latlongDriver.close();
                eventService.updateCityStatus(city, "DONE");
                LOGGER.info("SCRAPED CITY COUNT : " + cityCount + " , CURRENT CITY : " + city.getCity_Name());
                cityCount++;
                String file = eventService.createExcelFile(city.getCity_Name());
//                fileUpload.uploadToWhatsonyarravalley(driver,file);
            }

            LOGGER.info("SCRAPE FINISHED.");

        } finally {
            eventService.resetCities();
        }

    }

    private City findAvailableCity() throws Exception {
        return eventService.nextScrapeCity();
    }


    private void saveEvent(WebElement iuf4Uc, FirefoxDriver driver, String website, City city) throws Exception {

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

        SimpleDateFormat postDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String pDate = postDate.format(new Date());

        if (time.contains("AM") || time.contains("PM")) {
            SimpleDateFormat date12Format = new SimpleDateFormat("hh:mm a");
            SimpleDateFormat date24Format = new SimpleDateFormat("HH:mm:ss");


//            time = time.replace("AM", "").replace("PM", "");
//            time = time + ":00";
            time = date24Format.format(date12Format.parse(time)).replace(" ", "");
            LOGGER.info("TIME CONVERTED TO : - " + time);
        } else {
            time = "00:00:00";
        }

        time.replace(" ", "");
        formattedDate.replace(" ", "");

        Organizer organizer1 = getOrganizer(organizer, driver, website);
        String description = getDescription(website, driver);

        if (!description.contains("claim ownership")) {
            description = " [claim ownership] " + description;
        }

        if (!organizer1.getOrganizer_mobile().contains("+")) {
            organizer1.setOrganizer_mobile("+" + organizer1.getOrganizer_mobile().replace(" ", ""));
        }
        if (organizer1.getOrganizer_mobile().length() > 20) {
            organizer1.setOrganizer_mobile("");
        }
        if (organizer1.getOrganizer_mobile().length() < 9) {
            organizer1.setOrganizer_mobile("");
        }

        Event data = new Event();
        data.setAddress(address);
        data.setAlive_days("9");
        data.setCountry_id(city.getCountry_Id());
        data.setEnd_date(formattedDate);
        data.setGeo_latitude(latlong.split("@")[0]);
        data.setGeo_longitude(latlong.split("@")[1]);
        data.setMap_view(city.getMap_Type());
        data.setOrganizer_mobile(organizer1.getOrganizer_mobile().replace(":", ""));
        data.setOrganizer_name(organizer1.getOrganizer_name());
        data.setOrganizer_website(organizer1.getOrganizer_website());
        data.setPackage_id("55");
        data.setPost_city_id(city.getId() + "");
        data.setSt_date(formattedDate);
        data.setSt_time(time);
        data.setTemplatic_comment_status("open");
        data.setTemplatic_img(image);
        data.setTemplatic_ping_status("open");
        data.setTemplatic_post_author("1");
        data.setTemplatic_post_category("Events");
        data.setTemplatic_post_content(description);
        data.setTemplatic_post_date(pDate);
//        data.setTemplatic_post_date(formattedDate + " " + time);
        data.setTemplaticPostName(name);
        data.setTemplatic_post_status("publish");
        data.setTemplatic_post_title(name);
        data.setTemplatic_post_type("event");
        data.setZones_id(city.getZones_Id());
        eventService.saveEvent(data);

    }

    private String getDescription(String website, FirefoxDriver driver) throws Exception {
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
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM eventbrite.com.au");
            }
            return desc;


        } else if (website.contains("permaculturevictoria.org.au")) {
            LOGGER.warning("GETTING FROM : - permaculturevictoria.org.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[2]/div[3]/div/div/div/div/div/div")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM permaculturevictoria.org.au");
            }
            return desc;


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
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM meetup.com");
                }
            }
            return desc;


        } else if (website.contains("activeapril.vic.gov.au")) {
            LOGGER.warning("GETTING FROM : - activeapril.vic.gov.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[4]/div/div/div[1]/div/div[2]/div[1]/div/div/div[1]/p")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[4]/div/div/div[1]/div/div[2]/div[1]/div/div/div[1]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM activeapril.vic.gov.au");
                }
            }
            return desc;


        } else if (website.contains("kinglakeranges.com.au")) {
            LOGGER.info("GETTING FROM : - kinglakeranges.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div/div/div/div[1]/article/div[2]/div/div[3]/p[5]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[1]/div/div/div/div/div[1]/article/div[2]/div/div[3]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM kinglakeranges.com.au");
                }
            }
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
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM evensi.com");
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
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM yourlibrary.com.au");
            }
            return desc;


        } else if (website.contains("onlymelbourne.com.au")) {
            LOGGER.warning("GETTING FROM : - onlymelbourne.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div[3]/div[1]/div/div[2]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[1]/div[3]/div[1]/div")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM onlymelbourne.com.au");
                }
            }
            return desc;


        } else if (website.contains("maroondah.vic.gov.au")) {
            LOGGER.warning("GETTING FROM : - maroondah.vic.gov.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/form/div[3]/div[1]/div[2]/main/div/div[1]/div[4]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/form/div[3]/div[1]/div[2]/main/div/div[1]/div[2]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException v) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM maroondah.vic.gov.au");
                }
            }
            return desc;


        } else if (website.contains("gawler.org")) {
            LOGGER.info("GETTING FROM : - gawler.org");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div/div/section/main/div[1]/article/div[1]/table/tbody/tr[3]/td[2]/span")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM gawler.org");
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
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM bandsintown.com");
            }
            return desc;


        } else if (website.contains("zumba.com")) {
            LOGGER.warning("GETTING FROM : - zumba.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[2]/div/div[1]/div/div[4]/article/div[2]/p[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM zumba.com");
            }
            return desc;
        } else if (website.contains("eventful.com")) {
            LOGGER.info("GETTING FROM : - eventful.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div[4]/div[2]/div[1]/div[4]/div[1]/p[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                LOGGER.warning("FAILED TO RETRIEVE DESC FROM eventful.com");
            }
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
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM facebook.com");
                }
            }
            return desc;


        } else if (website.contains("vicparks.com.au")) {
            LOGGER.warning("GETTING FROM : - vicparks.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[4]/div/main/div/div[2]/div[4]/div[2]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[4]/div/main/div/div[2]/div[4]/div[2]/p[2]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM vicparks.com.au");
                }
            }
            return desc;


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
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM thehomehotel.net.au");
                }
            }
            return desc;


        } else if (website.contains("whittleseacountrymusicfestival.com.au")) {
            LOGGER.warning("GETTING FROM : - whittleseacountrymusicfestival.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div[1]/p[3]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("//*[@id=\"content-container-pro\"]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM  whittleseacountrymusicfestival.com.au");
                }
            }
            return desc;


        } else if (website.contains("boroondara.vic.gov.au")) {
            LOGGER.warning("GETTING FROM  : - boroondara.vic.gov.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div[1]/main/div[2]/div/div/div/div/div/div/div/div[1]/div[2]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[1]/div/div[1]/main/div[2]/div/div/div/div/div/div/div/div[1]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM boroondara.vic.gov.au");
                }
            }
            return desc;


        } else if (website.contains("nunawadingswimmingclub.com")) {
            LOGGER.warning("GETTING FROM : - nunawadingswimmingclub.com");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[2]/div/main/div/div[2]/div[3]/div[3]/div[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[2]/div/main/div/div[2]/div[3]/div[3]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM nunawadingswimmingclub.com");
                }
            }
            return desc;


        } else if (website.contains("avocapark.com.au")) {
            LOGGER.warning("GETTING FROM : - avocapark.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/div[1]/div/div/div[2]/div[4]/div[1]/div/div/div[2]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/div[1]/div/div/div[2]/div[4]/div[1]/div/div/div[1]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM avocapark.com.au");
                }
            }
            return desc;


        } else if (website.contains("fredstinyhouses.com.au")) {
            LOGGER.warning("GETTING FROM : - fredstinyhouses.com.au");
            driver.get(website);
            String desc = "--";
            try {
                desc = driver.findElementByXPath("/html/body/section[1]/div/div[1]/main/article/div[1]/div[2]/div[3]/div[2]/p[1]")
                        .getAttribute("innerText");
                System.err.println(desc);
            } catch (Exception e) {
                try {
                    desc = driver.findElementByXPath("/html/body/section[1]/div/div[1]/main/article/div[1]/div[2]/div[3]/div[2]")
                            .getAttribute("innerText");
                    System.err.println(desc);
                } catch (NoSuchElementException f) {
                    LOGGER.warning("FAILED TO RETRIEVE DESC FROM fredstinyhouses.com.au");
                }
            }
            return desc;


        } else if (website.contains("fieldandgame.com.au")) {
            LOGGER.warning("SKIPPING DESCRIPTION SITE : - fieldandgame.com.au");
            driver.get(website);
            String desc = "--";
            return desc;


        } else if (website.contains("songkick.com")) {
            LOGGER.warning("SKIPPING DESCRIPTION SITE : - songkick.com");
            driver.get(website);
            String desc = "--";
            return desc;


        } else {
            LOGGER.warning("SITE NOT IN EXPECTED LIST");
            return "--";
        }

    }

    private Organizer getOrganizer(String organizer, FirefoxDriver driver, String website) throws Exception {
        driver.get(organizer);
        Organizer organizer1 = new Organizer();
        String phone = "";
        String organizer_name = "";
        try {
            WebElement mainDiv = driver.findElementByXPath("//*[@id=\"rhs_block\"]");
            organizer_name = mainDiv.findElement(By.tagName("div")).findElements(By.xpath("./*")).get(0).findElement(By.tagName("div"))//xpdopen
                    .findElements(By.xpath("./*")).get(0).findElements(By.xpath("./*")).get(1)                              //<div data-ved="2ahUKEwjs28OImqzgAhXLRY8KHV5DCFcQ_xd6BAgXEAI">
                    .findElements(By.xpath("./*")).get(1).findElement(By.tagName("div")).findElements(By.xpath("./*")).get(1)
                    .findElements(By.xpath("./*")).get(0).findElement(By.tagName("div")).findElement(By.tagName("div"))
                    .findElements(By.xpath("./*")).get(0).getAttribute("innerText");


            for (WebElement element : mainDiv.findElement(By.tagName("div")).findElements(By.xpath("./*")).get(0).findElement(By.tagName("div"))//xpdopen
                    .findElements(By.xpath("./*")).get(0).findElements(By.xpath("./*")).get(1)                              //<div data-ved="2ahUKEwjs28OImqzgAhXLRY8KHV5DCFcQ_xd6BAgXEAI">
                    .findElements(By.xpath("./*"))) {

                String innerHTML = element.getAttribute("innerText");
                if (innerHTML.contains("Phone")) {
                    phone = innerHTML.split("Phone")[1];
                    break;
                }

            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warning("IndexOutOfBoundsException  - Method getOrganizer. line 680");
        }
        organizer1.setOrganizer_mobile(phone);
        organizer1.setOrganizer_name(organizer_name);
        organizer1.setOrganizer_website(website);

        return organizer1;
    }

    private String fixDateFormat(String fullMonth, String year) throws Exception {
        String date = "";
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

        date = year + "-" + month + "-" + day;
        return date.trim();

    }

    private String saveImage(FirefoxDriver driver, String event) throws Exception {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String folder = dtf.format(now);
        folder = folder.split(" ")[0];
        folder = folder.replace("/", "_");

        File dir = new File("/var/lib/tomcat8/bulk/" + folder);
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (SecurityException e) {
                System.out.println("can not create directory");
            }
        }


        for (WebElement nav : driver.findElementByXPath("//*[@id=\"hdtb-msb-vis\"]").findElements(By.xpath("./*"))) {
            try {
                if (nav.findElement(By.tagName("a")).getAttribute("innerText").equalsIgnoreCase("Images")) {
                    driver.get(nav.findElement(By.tagName("a")).getAttribute("href"));
                    break;
                }
            } catch (NoSuchElementException e) {
                LOGGER.warning("NSE EXCEPTION  - Method save image. clicking on navigation for images. line 712");
                continue;
            }
        }
        try {
            WebElement rgs = driver.findElement(By.id("rg_s"));
//            List<WebElement> images = rgs.findElements(By.tagName("div"));
            List<WebElement> aTg = rgs.findElements(By.tagName("a"));
            aTg.get(0).click();
        } catch (Exception e) {
            e.printStackTrace();
            return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/event.jpg";
        }


        boolean secoundChance = false;
        while (true) {
            System.out.println("WHILE LOOP IN IMAGE SAVE METHOD");
            if (secoundChance) {
                try {
                    driver.findElementByXPath("//*[@id=\"rg_s\"]").findElements(By.xpath("./*")).get(0)
                            .findElements(By.xpath("./*")).get(1).click();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/event.jpg";
                }
            }


            Thread.sleep(3000);
            WebElement bigImage = driver.findElementByXPath("//*[@id=\"irc_bg\"]");
            String src = bigImage.findElement(By.id("irc-cl")).findElement(By.id("irc_cc"))
                    .findElements(By.xpath("./*")).get(1).findElements(By.xpath("./*")).get(0)
                    .findElement(By.className("irc_mic")).findElements(By.xpath("./*")).get(0)
                    .findElement(By.tagName("a")).findElement(By.tagName("img")).getAttribute("src");
            event = event.replace("-", "_");
            event = event.replace("/", "_");
            LOGGER.info("SAVING IMAGE URL " + src);
            URLConnection con = null;
            InputStream in = null;
            try {
                URL imageUrl = new URL(src);
                con = imageUrl.openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                in = con.getInputStream();
                BufferedImage saveImage = ImageIO.read(in);

                if (saveImage != null) {
                    System.err.println("IMAGE LOADED");
                } else {
                    System.err.println("IMAGE NOT LOADED");
                }

//                BufferedImage saveImage = ImageIO.read(imageUrl);
                BufferedImage newBufferedImage = new BufferedImage(saveImage.getWidth(),
                        saveImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                newBufferedImage.createGraphics().drawImage(saveImage, 0, 0, Color.WHITE, null);
                ImageIO.write(newBufferedImage, "jpg", new File("/var/lib/tomcat8/bulk/" + folder + "/" + event + ".jpg"));
                saveImage.flush();
                newBufferedImage.flush();
                System.err.println("IMAGE SAVED");
                return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/" + folder + "/" + event + ".jpg";

            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warning("ERROR IN IMAGE SAVE METHOD.  | LINE 739 " + e.getMessage());
                if (!secoundChance) {
                    secoundChance = true;
                    LOGGER.info("TARING TO SECOND IMAGE");
                    continue;
                } else {
                    return "https://www.whatsonyarravalley.com.au/wp-content/uploads/bulk/event.jpg";
                }
            }
        }
    }

    public String getLatitude(String address) throws Exception {

        String latitude = "";
        String longitude = "";
        int count = 0;

        while (true) {
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
