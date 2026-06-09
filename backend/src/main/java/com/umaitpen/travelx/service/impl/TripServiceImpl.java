package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.repository.TripRepository;
import com.umaitpen.travelx.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
    private final TripRepository tripRepository;

    @Override
    public Trip createTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    @Override
    public Trip getTrip(Long id) {
        return tripRepository.findById(id).orElse(null);
    }

    @Override
    public List<Trip> listByOwner(Long ownerId) {
        return tripRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Trip> findAll() {
        return tripRepository.findAll();
    }
}
