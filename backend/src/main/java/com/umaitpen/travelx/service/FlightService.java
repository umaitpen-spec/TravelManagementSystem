package com.umaitpen.travelx.service;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.model.Flight;

import java.time.LocalDate;
import java.util.List;

public interface FlightService {
    Flight create(Flight flight, Long providerId);
    Flight update(Long id, Flight flight);
    void delete(Long id);
    List<Flight> getByProvider(Long providerId);
    List<Flight> getAll();
    Flight getById(Long id);
    Flight getFlight(Long id);
    List<Flight> getByOrigin(String origin);
    List<Flight> getByDestination(String destination);
    List<Flight> searchFlights(String source, String destination, LocalDate departureDate);
    Flight updateStatus(Long id, FlightStatus status);
    List<Flight> getActiveFlights();
}