package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.model.Booking;
import com.umaitpen.travelx.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> addBooking(@PathVariable Long tripId, @RequestBody Booking booking) {
        Booking saved = bookingService.addBooking(tripId, booking);
        if (saved == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getBookings(@PathVariable Long tripId) {
        return ResponseEntity.ok(bookingService.getBookingsForTrip(tripId));
    }
}
