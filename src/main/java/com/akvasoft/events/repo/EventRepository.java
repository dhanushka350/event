package com.akvasoft.events.repo;

import com.akvasoft.events.modal.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    Event getTopByTemplaticPostNameEquals(String name);
}
