package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.Trip;

import java.util.List;

public interface TripService {
    Trip createTrip(Trip trip);
    Trip getTrip(Long id);
    List<Trip> listByOwner(Long ownerId);
    List<Trip> findAll();
}
