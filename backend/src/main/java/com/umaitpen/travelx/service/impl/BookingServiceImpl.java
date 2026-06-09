package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Booking;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.repository.BookingRepository;
import com.umaitpen.travelx.repository.TripRepository;
import com.umaitpen.travelx.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;

    @Override
    public Booking addBooking(Long tripId, Booking booking) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return null;
        }
        booking.setTrip(trip);
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getBookingsForTrip(Long tripId) {
        return bookingRepository.findByTripId(tripId);
    }
}
