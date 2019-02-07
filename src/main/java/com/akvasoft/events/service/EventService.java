package com.akvasoft.events.service;

import com.akvasoft.events.modal.Event;
import com.akvasoft.events.repo.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public boolean saveEvent(Event event) {
        Event equals = eventRepository.getTopByNameEquals(event.getName());
        if (equals != null) {
            event.setId(equals.getId());
            System.out.println("===========================================================");
            System.out.println("=======================EVENT UPDATED=======================");
            System.out.println("===========================================================");
        }
        eventRepository.save(event);
        return true;
    }
}
