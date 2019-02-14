package com.akvasoft.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class EventsApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(EventsApplication.class, args);
    }

}

