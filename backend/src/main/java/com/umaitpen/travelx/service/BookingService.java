package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.Booking;

import java.util.List;

public interface BookingService {
    Booking addBooking(Long tripId, Booking booking);
    List<Booking> getBookingsForTrip(Long tripId);
}
