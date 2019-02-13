package com.akvasoft.events.repo;

import com.akvasoft.events.modal.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepo extends JpaRepository<City, Integer> {
    City findTopByStatusEquals(String status);
}
