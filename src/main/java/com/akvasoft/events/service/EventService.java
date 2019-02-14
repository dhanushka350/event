package com.akvasoft.events.service;

import com.akvasoft.events.dto.ExcelData;
import com.akvasoft.events.modal.City;
import com.akvasoft.events.modal.Event;
import com.akvasoft.events.repo.CityRepo;
import com.akvasoft.events.repo.EventRepository;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CityRepo cityRepo;
    private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public boolean saveEvent(Event event) throws Exception {
        Event equals = eventRepository.getTopByTemplaticPostNameEquals(event.getTemplaticPostName());
        if (equals != null) {
            event.setId(equals.getId());
            System.out.println("===========================================================");
            System.out.println("=======================EVENT UPDATED=======================");
            System.out.println("===========================================================");
        } else {
            System.out.println("===========================================================");
            System.out.println("========================EVENT SAVED========================");
            System.out.println("===========================================================");
        }
        eventRepository.save(event);
        return true;
    }

    public List<City> getAllCities() {
        return cityRepo.findAll();
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public City nextScrapeCity() throws Exception {
        System.out.println("888888888888888888");
        readWriteLock.writeLock().lock();
        City pending = cityRepo.findTopByStatusEquals("PENDING");
        updateCityStatus(pending, "SCRAPING");
        readWriteLock.writeLock().unlock();
        return pending;
    }

    public void updateCityStatus(City city, String status) throws Exception {
        if (city != null) {
            city.setStatus(status);
            cityRepo.saveAndFlush(city);
        }
    }

    public void createExcelFile() throws Exception {

        List<Event> datalist = eventRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        CreationHelper createHelper = workbook.getCreationHelper();
        Sheet sheet = workbook.createSheet("EVENTS");
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setColor(IndexedColors.BLACK.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(1);
        ArrayList<String> list = new ArrayList();

        list.add("templatic_post_author");
        list.add("templatic_post_date");
        list.add("templatic_post_title");
        list.add("templatic_post_category");
        list.add("templatic_img");
        list.add("templatic_post_content");
        list.add("templatic_post_status");
        list.add("templatic_comment_status");
        list.add("templatic_ping_status");
        list.add("templatic_post_name");
        list.add("templatic_post_type");
        list.add("post_city_id");
        list.add("map_view");
        list.add("address");
        list.add("st_date");
        list.add("end_date");
        list.add("st_time");
        list.add("organizer_name");
        list.add("organizer_website");
        list.add("organizer_mobile");
        list.add("country_id");
        list.add("zones_id");
        list.add("alive_days");
        list.add("geo_latitude");
        list.add("geo_longitude");
        list.add("package_id ");

        for (int i = 0; i < list.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(list.get(i));
            cell.setCellStyle(headerCellStyle);
        }

        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        int rows = 2;
        for (Event data : datalist) {
            Row row = sheet.createRow(rows++);
            row.createCell(0).setCellValue(data.getTemplatic_post_author());
            row.createCell(1).setCellValue(data.getTemplatic_post_date());
            row.createCell(2).setCellValue(data.getTemplatic_post_title());
            row.createCell(3).setCellValue(data.getTemplatic_post_category());
            row.createCell(4).setCellValue(data.getTemplatic_img());
            row.createCell(5).setCellValue(data.getTemplatic_post_content());
            row.createCell(6).setCellValue(data.getTemplatic_post_status());
            row.createCell(7).setCellValue(data.getTemplatic_comment_status());
            row.createCell(8).setCellValue(data.getTemplatic_ping_status());
            row.createCell(9).setCellValue(data.getTemplaticPostName());
            row.createCell(10).setCellValue(data.getTemplatic_post_type());
            row.createCell(11).setCellValue(data.getPost_city_id());
            row.createCell(12).setCellValue(data.getMap_view());
            row.createCell(13).setCellValue(data.getAddress());
            row.createCell(14).setCellValue(data.getSt_date());
            row.createCell(15).setCellValue(data.getEnd_date());
            row.createCell(16).setCellValue(data.getSt_time());
            row.createCell(17).setCellValue(data.getOrganizer_name());
            row.createCell(18).setCellValue(data.getOrganizer_website());
            row.createCell(19).setCellValue(data.getOrganizer_mobile());
            row.createCell(20).setCellValue(data.getCountry_id());
            row.createCell(21).setCellValue(data.getZones_id());
            row.createCell(22).setCellValue(data.getAlive_days());
            row.createCell(23).setCellValue(data.getGeo_latitude());
            row.createCell(24).setCellValue(data.getGeo_longitude());
            row.createCell(25).setCellValue(data.getPackage_id());
        }

        FileOutputStream fileOut = new FileOutputStream("/var/lib/tomcat8/EVENTS.xlsx");
        workbook.write(fileOut);
        fileOut.close();
        System.out.println("Excel Created");

        CSVWriter csvWriter = new CSVWriter(new FileWriter("/var/lib/tomcat8/EVENTS.csv"));
        List<String[]> csvRows = new LinkedList<String[]>();
        csvRows.add(new String[]
                {
                        "templatic_post_author",
                        "templatic_post_date",
                        "templatic_post_title",
                        "templatic_post_category",
                        "templatic_img",
                        "templatic_post_tags",
                        "templatic_post_content",
                        "templatic_post_excerpt",
                        "templatic_post_status",
                        "templatic_comment_status",
                        "templatic_ping_status",
                        "templatic_post_name",
                        "templatic_post_type",
                        "templatic_post_comment_count",
                        "templatic_comments_data",
                        "post_city_id",
                        "address",
                        "geo_latitude",
                        "geo_longitude",
                        "map_view",
                        "event_type",
                        "st_date",
                        "end_date",
                        "st_time",
                        "end_time",
                        "reg_fees",
                        "reg_desc",
                        "phone",
                        "post_tags",
                        "email",
                        "website",
                        "twitter",
                        "facebook",
                        "google_plus",
                        "organizer_name",
                        "organizer_email",
                        "organizer_logo",
                        "organizer_address",
                        "organizer_contact",
                        "organizer_website",
                        "organizer_mobile",
                        "organizer_desc",
                        "video",
                        "country_id",
                        "zones_id",
                        "recurrence_occurs",
                        "recurrence_per",
                        "recurrence_days",
                        "recurrence_bydays",
                        "monthly_recurrence_byweekno",
                        "recurrence_byday",
                        "set_st_time",
                        "set_end_time",
                        "alive_days",
                        "featured_type",
                        "package_id"

                });

        for (Event data : datalist) {
            csvRows.add(new String[]
                    {
                            data.getTemplatic_post_author(),
                            data.getTemplatic_post_date(),
                            data.getTemplatic_post_title(),
                            data.getTemplatic_post_category(),
                            data.getTemplatic_img(),
                            "",//templatic_post_tags
                            data.getTemplatic_post_content(),
                            "",//templatic_post_excerpt
                            data.getTemplatic_post_status(),
                            data.getTemplatic_comment_status(),
                            data.getTemplatic_ping_status(),
                            data.getTemplaticPostName(),
                            data.getTemplatic_post_type(),
                            "0",//templatic_post_comment_count
                            "",//templatic_comments_data
                            data.getPost_city_id(),
                            data.getAddress(),
                            data.getGeo_latitude(),
                            data.getGeo_longitude(),
                            data.getMap_view(),
                            "Regular event",//event_type
                            data.getSt_date(),
                            data.getEnd_date(),
                            data.getSt_time(),
                            "",//end_time
                            "",//reg_fees
                            "",//reg_desc
                            "",//phone
                            "",//post_tags
                            "",//email
                            "",//website
                            "",//twitter
                            "",//facebook
                            "",//google_plus
                            data.getOrganizer_name(),
                            "",//organizer_email
                            "",//organizer_logo
                            "",//organizer_address
                            "",//organizer_contact
                            data.getOrganizer_website(),
                            data.getOrganizer_mobile(),
                            "",//organizer_desc
                            "",//video
                            data.getCountry_id(),
                            data.getZones_id(),
                            "",//recurrence_occurs
                            "",//recurrence_per
                            "",//recurrence_days
                            "",//recurrence_bydays
                            "",//monthly_recurrence_byweekno
                            "",//recurrence_byday
                            "",//set_st_time
                            "",//set_end_time
                            data.getAlive_days(),
                            "",//featured_type
                            data.getPackage_id()
                    });
        }
        csvWriter.writeAll(csvRows);
        csvWriter.close();
    }

    public void resetCities() throws Exception {
        List<City> list = new ArrayList<>();
        for (City city : cityRepo.findAll()) {
            city.setStatus("PENDING");
            list.add(city);
        }
        cityRepo.saveAll(list);
    }
}
