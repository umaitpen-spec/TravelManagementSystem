package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.FlightClass;
import com.umaitpen.travelx.repository.FlightClassRepository;
import com.umaitpen.travelx.repository.FlightRepository;
import com.umaitpen.travelx.service.FlightClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightClassServiceImpl implements FlightClassService {
    private final FlightClassRepository flightClassRepository;
    private final FlightRepository flightRepository;

    @Override
    @Transactional
    public FlightClass create(FlightClass flightClass, Long flightId) {
        Flight flight = flightRepository.findById(flightId).orElse(null);
        if (flight == null) return null;
        flightClass.setFlight(flight);
        return flightClassRepository.save(flightClass);
    }

    @Override
    @Transactional
    public FlightClass update(Long id, FlightClass flightClass) {
        FlightClass existing = flightClassRepository.findById(id).orElse(null);
        if (existing == null) return null;
        existing.setClassType(flightClass.getClassType());
        existing.setTotalSeats(flightClass.getTotalSeats());
        existing.setAvailableSeats(flightClass.getAvailableSeats());
        existing.setPrice(flightClass.getPrice());
        existing.setBaggageLimit(flightClass.getBaggageLimit());
        existing.setRefundable(flightClass.isRefundable());
        return flightClassRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        flightClassRepository.deleteById(id);
    }

    @Override
    public List<FlightClass> getByFlight(Long flightId) {
        return flightClassRepository.findByFlightId(flightId);
    }

    @Override
    public FlightClass getById(Long id) {
        return flightClassRepository.findById(id).orElse(null);
    }
}
