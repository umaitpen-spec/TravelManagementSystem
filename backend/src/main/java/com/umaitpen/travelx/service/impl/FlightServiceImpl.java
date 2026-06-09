package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.FlightRepository;
import com.umaitpen.travelx.repository.UserRepository;
import com.umaitpen.travelx.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;

    @Override
    public Flight create(Flight flight, Long providerId) {
        User provider = userRepository.findById(providerId).orElse(null);
        if (provider != null) {
            flight.setProvider(provider);
        }
        flight.setStatus(FlightStatus.ACTIVE);
        return flightRepository.save(flight);
    }

    @Override
    @Transactional
    public Flight update(Long id, Flight flight) {
        Flight existing = flightRepository.findById(id).orElse(null);
        if (existing == null) return null;
        existing.setAirline(flight.getAirline());
        existing.setFlightNumber(flight.getFlightNumber());
        existing.setOrigin(flight.getOrigin());
        existing.setDestination(flight.getDestination());
        existing.setSourceCity(flight.getSourceCity());
        existing.setDestinationCity(flight.getDestinationCity());
        existing.setDepartureDate(flight.getDepartureDate());
        existing.setDepartureTime(flight.getDepartureTime());
        existing.setArrivalTime(flight.getArrivalTime());
        existing.setDuration(flight.getDuration());
        existing.setFlightType(flight.getFlightType());
        existing.setBaggageDetails(flight.getBaggageDetails());
        existing.setDescription(flight.getDescription());
        existing.setImageUrl(flight.getImageUrl());
        existing.setAirlineLogo(flight.getAirlineLogo());
        existing.setAmenities(flight.getAmenities());
        existing.setCancellationPolicy(flight.getCancellationPolicy());
        existing.setStatus(flight.getStatus());
        return flightRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        flightRepository.deleteById(id);
    }

    @Override
    public List<Flight> getByProvider(Long providerId) {
        return flightRepository.findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    @Override
    public List<Flight> getAll() {
        return flightRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Flight getById(Long id) {
        return flightRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Flight getFlight(Long id) {
        return flightRepository.findByIdWithClasses(id);
    }

    @Override
    public List<Flight> getByOrigin(String origin) {
        return flightRepository.findAll().stream()
                .filter(f -> f.getSourceCity() != null && f.getSourceCity().equalsIgnoreCase(origin))
                .toList();
    }

    @Override
    public List<Flight> getByDestination(String destination) {
        return flightRepository.findAll().stream()
                .filter(f -> f.getDestinationCity() != null && f.getDestinationCity().equalsIgnoreCase(destination))
                .toList();
    }

    @Override
    public List<Flight> searchFlights(String source, String destination, LocalDate departureDate) {
        return flightRepository.searchFlights(source, destination, departureDate);
    }

    @Override
    @Transactional
    public Flight updateStatus(Long id, FlightStatus status) {
        Flight flight = flightRepository.findById(id).orElse(null);
        if (flight == null) return null;
        flight.setStatus(status);
        return flightRepository.save(flight);
    }

    @Override
    public List<Flight> getActiveFlights() {
        return flightRepository.findByStatus(FlightStatus.ACTIVE);
    }
}
