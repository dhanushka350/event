package com.akvasoft.events.modal;

import javax.persistence.*;

@Entity
@Table(name = "city")
public class City {
    @Id
    @Column(name = "City_Id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "Country_Id")
    private String Country_Id;

    @Column(name = "Zones_Id")
    private String Zones_Id;

    @Column(name = "City_Name")
    private String City_Name;

    @Column(name = "City_Slug")
    private String City_Slug;

    @Column(name = "Latitude")
    private String Latitude;

    @Column(name = "Longitude")
    private String Longitude;

    @Column(name = "Scall_Factor")
    private String Scall_Factor;

    @Column(name = "Is_Zoom_Home")
    private String Is_Zoom_Home;

    @Column(name = "Map_Type")
    private String Map_Type;

    @Column(name = "Post_Type")
    private String Post_Type;

    @Column(name = "Category_ids")
    private String Category_ids;

    @Column(name = "Is_Default")
    private String Is_Default;

    @Column(name = "Message")
    private String Message;

    @Column(name = "Color")
    private String Color;

    @Column(name = "Image")
    private String Image;

    @Column(name = "header_color")
    private String header_color;

    @Column(name = "header_image")
    private String header_image;

    @Column(name = "status")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry_Id() {
        return Country_Id;
    }

    public void setCountry_Id(String country_Id) {
        Country_Id = country_Id;
    }

    public String getZones_Id() {
        return Zones_Id;
    }

    public void setZones_Id(String zones_Id) {
        Zones_Id = zones_Id;
    }

    public String getCity_Name() {
        return City_Name;
    }

    public void setCity_Name(String city_Name) {
        City_Name = city_Name;
    }

    public String getCity_Slug() {
        return City_Slug;
    }

    public void setCity_Slug(String city_Slug) {
        City_Slug = city_Slug;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getScall_Factor() {
        return Scall_Factor;
    }

    public void setScall_Factor(String scall_Factor) {
        Scall_Factor = scall_Factor;
    }

    public String getIs_Zoom_Home() {
        return Is_Zoom_Home;
    }

    public void setIs_Zoom_Home(String is_Zoom_Home) {
        Is_Zoom_Home = is_Zoom_Home;
    }

    public String getMap_Type() {
        return Map_Type;
    }

    public void setMap_Type(String map_Type) {
        Map_Type = map_Type;
    }

    public String getPost_Type() {
        return Post_Type;
    }

    public void setPost_Type(String post_Type) {
        Post_Type = post_Type;
    }

    public String getCategory_ids() {
        return Category_ids;
    }

    public void setCategory_ids(String category_ids) {
        Category_ids = category_ids;
    }

    public String getIs_Default() {
        return Is_Default;
    }

    public void setIs_Default(String is_Default) {
        Is_Default = is_Default;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public String getColor() {
        return Color;
    }

    public void setColor(String color) {
        Color = color;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public String getHeader_color() {
        return header_color;
    }

    public void setHeader_color(String header_color) {
        this.header_color = header_color;
    }

    public String getHeader_image() {
        return header_image;
    }

    public void setHeader_image(String header_image) {
        this.header_image = header_image;
    }
}
