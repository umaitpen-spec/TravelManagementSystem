package com.umaitpen.travelx.service;

import com.umaitpen.travelx.dto.CreateBookingRequest;
import com.umaitpen.travelx.dto.HotelBookingDTO;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.HotelBooking;

import java.util.List;

public interface HotelBookingService {
    HotelBooking createBooking(Long userId, CreateBookingRequest request);
    HotelBooking updateStatus(Long bookingId, BookingStatus status);
    List<HotelBooking> getByUser(Long userId);
    List<HotelBooking> getByProvider(Long providerId);
    List<HotelBooking> getByHotel(Long hotelId);
    List<HotelBooking> getAll();
    HotelBooking getById(Long id);
    double getTotalRevenueForProvider(Long providerId);
    int getBookingCountForProvider(Long providerId);
}
