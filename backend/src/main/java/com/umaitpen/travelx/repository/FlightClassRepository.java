package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.model.FlightClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightClassRepository extends JpaRepository<FlightClass, Long> {
    List<FlightClass> findByFlightId(Long flightId);
    List<FlightClass> findByFlightIdAndAvailableSeatsGreaterThan(Long flightId, Integer count);
    void deleteByFlightId(Long flightId);
}
