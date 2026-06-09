package com.umaitpen.travelx.service;

import com.umaitpen.travelx.dto.CreateFlightBookingRequest;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.FlightBooking;

import java.util.List;

public interface FlightBookingService {
    FlightBooking createBooking(Long userId, CreateFlightBookingRequest request);
    FlightBooking updateStatus(Long bookingId, BookingStatus status);
    List<FlightBooking> getByUser(Long userId);
    List<FlightBooking> getByProvider(Long providerId);
    List<FlightBooking> getByFlight(Long flightId);
    List<FlightBooking> getAll();
    FlightBooking getById(Long id);
    double getTotalRevenueForProvider(Long providerId);
    int getBookingCountForProvider(Long providerId);
    int getSeatsFilledForProvider(Long providerId);
}