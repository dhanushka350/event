package com.akvasoft.events.dto;

public class Organizer {
    private String organizer_name;
    private String organizer_website;
    private String organizer_mobile;

    public String getOrganizer_name() {
        if (organizer_name == null) {
            organizer_name = "-";
        }
        return organizer_name;
    }

    public void setOrganizer_name(String organizer_name) {
        this.organizer_name = organizer_name;
    }

    public String getOrganizer_website() {
        if (organizer_website == null) {
            organizer_website = "-";
        }
        return organizer_website;
    }

    public void setOrganizer_website(String organizer_website) {
        this.organizer_website = organizer_website;
    }

    public String getOrganizer_mobile() {
        System.out.println("*********************************************************************** " + organizer_mobile);
        if (organizer_mobile == null) {
            organizer_mobile = "-";
        }
        return organizer_mobile;
    }

    public void setOrganizer_mobile(String organizer_mobile) {
        this.organizer_mobile = organizer_mobile;
    }
}
