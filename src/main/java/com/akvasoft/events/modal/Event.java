package com.akvasoft.events.modal;

import com.beust.jcommander.converters.URIConverter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "T_EVENT")
public class Event {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "templatic_post_author")
    private String templatic_post_author;

    @Column(name = "templatic_post_date")
    private String templatic_post_date;

    @Column(name = "templatic_post_title")
    private String templatic_post_title;

    @Column(name = "templatic_post_category")
    private String templatic_post_category;

    @Column(name = "templatic_img", length = 2500)
    private String templatic_img;

    @Column(name = "templatic_post_content", length = 5000)
    private String templatic_post_content;

    @Column(name = "templatic_post_status")
    private String templatic_post_status;

    @Column(name = "templatic_comment_status")
    private String templatic_comment_status;

    @Column(name = "templatic_ping_status")
    private String templatic_ping_status;

    @Column(name = "templatic_post_name")
    private String templaticPostName;

    @Column(name = "templatic_post_type")
    private String templatic_post_type;

    @Column(name = "post_city_id")
    private String post_city_id;

    @Column(name = "map_view")
    private String map_view;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "st_date")
    private String st_date;

    @Column(name = "end_date")
    private String end_date;

    @Column(name = "st_time")
    private String st_time;

    @Column(name = "organizer_name")
    private String organizer_name;

    @Column(name = "organizer_website", length = 5000)
    private String organizer_website;

    @Column(name = "organizer_mobile")
    private String organizer_mobile;

    @Column(name = "country_id")
    private String country_id;

    @Column(name = "zones_id")
    private String zones_id;

    @Column(name = "alive_days")
    private String alive_days;

    @Column(name = "geo_latitude")
    private String geo_latitude;

    @Column(name = "geo_longitude")
    private String geo_longitude;

    @Column(name = "package_id")
    private String package_id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTemplatic_post_author() {
        return templatic_post_author;
    }

    public void setTemplatic_post_author(String templatic_post_author) {
        this.templatic_post_author = templatic_post_author;
    }

    public String getTemplatic_post_date() {
        return templatic_post_date;
    }

    public void setTemplatic_post_date(String templatic_post_date) {
        this.templatic_post_date = templatic_post_date;
    }

    public String getTemplatic_post_title() {
        return templatic_post_title;
    }

    public void setTemplatic_post_title(String templatic_post_title) {
        this.templatic_post_title = templatic_post_title;
    }

    public String getTemplatic_post_category() {
        return templatic_post_category;
    }

    public void setTemplatic_post_category(String templatic_post_category) {
        this.templatic_post_category = templatic_post_category;
    }

    public String getTemplatic_img() {
        return templatic_img;
    }

    public void setTemplatic_img(String templatic_img) {
        this.templatic_img = templatic_img;
    }

    public String getTemplatic_post_content() {
        return templatic_post_content;
    }

    public void setTemplatic_post_content(String templatic_post_content) {
        if (templatic_post_content.length() > 5000) {
            templatic_post_content = templatic_post_content.substring(0, 4990);
        }
        this.templatic_post_content = templatic_post_content;
    }

    public String getTemplatic_post_status() {
        return templatic_post_status;
    }

    public void setTemplatic_post_status(String templatic_post_status) {
        this.templatic_post_status = templatic_post_status;
    }

    public String getTemplatic_comment_status() {
        return templatic_comment_status;
    }

    public void setTemplatic_comment_status(String templatic_comment_status) {
        this.templatic_comment_status = templatic_comment_status;
    }

    public String getTemplatic_ping_status() {
        return templatic_ping_status;
    }

    public void setTemplatic_ping_status(String templatic_ping_status) {
        this.templatic_ping_status = templatic_ping_status;
    }

    public String getTemplaticPostName() {
        return templaticPostName;
    }

    public void setTemplaticPostName(String templaticPostName) {
        this.templaticPostName = templaticPostName;
    }

    public String getTemplatic_post_type() {
        return templatic_post_type;
    }

    public void setTemplatic_post_type(String templatic_post_type) {
        this.templatic_post_type = templatic_post_type;
    }

    public String getPost_city_id() {
        return post_city_id;
    }

    public void setPost_city_id(String post_city_id) {
        this.post_city_id = post_city_id;
    }

    public String getMap_view() {
        return map_view;
    }

    public void setMap_view(String map_view) {
        this.map_view = map_view;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSt_date() {
        return st_date;
    }

    public void setSt_date(String st_date) {
        this.st_date = st_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String getSt_time() {
        return st_time;
    }

    public void setSt_time(String st_time) {
        this.st_time = st_time;
    }

    public String getOrganizer_name() {
        return organizer_name;
    }

    public void setOrganizer_name(String organizer_name) {
        this.organizer_name = organizer_name;
    }

    public String getOrganizer_website() {
        return organizer_website;
    }

    public void setOrganizer_website(String organizer_website) {
        this.organizer_website = organizer_website;
    }

    public String getOrganizer_mobile() {
        return organizer_mobile;
    }

    public void setOrganizer_mobile(String organizer_mobile) {
        this.organizer_mobile = organizer_mobile;
    }

    public String getCountry_id() {
        return country_id;
    }

    public void setCountry_id(String country_id) {
        this.country_id = country_id;
    }

    public String getZones_id() {
        return zones_id;
    }

    public void setZones_id(String zones_id) {
        this.zones_id = zones_id;
    }

    public String getAlive_days() {
        return alive_days;
    }

    public void setAlive_days(String alive_days) {
        this.alive_days = alive_days;
    }

    public String getGeo_latitude() {
        return geo_latitude;
    }

    public void setGeo_latitude(String geo_latitude) {
        this.geo_latitude = geo_latitude;
    }

    public String getGeo_longitude() {
        return geo_longitude;
    }

    public void setGeo_longitude(String geo_longitude) {
        this.geo_longitude = geo_longitude;
    }

    public String getPackage_id() {
        return package_id;
    }

    public void setPackage_id(String package_id) {
        this.package_id = package_id;
    }
}
