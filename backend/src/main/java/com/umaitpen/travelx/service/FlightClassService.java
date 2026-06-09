package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.FlightClass;

import java.util.List;

public interface FlightClassService {
    FlightClass create(FlightClass flightClass, Long flightId);
    FlightClass update(Long id, FlightClass flightClass);
    void delete(Long id);
    List<FlightClass> getByFlight(Long flightId);
    FlightClass getById(Long id);
}
