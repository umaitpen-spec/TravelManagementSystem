package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.CreateBookingRequest;
import com.umaitpen.travelx.dto.HotelBookingDTO;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.HotelBooking;
import com.umaitpen.travelx.repository.HotelBookingRepository;
import com.umaitpen.travelx.service.HotelBookingService;
import com.umaitpen.travelx.service.HotelImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class HotelBookingController {
    private final HotelBookingService hotelBookingService;
    private final HotelBookingRepository hotelBookingRepository;
    private final HotelImageService hotelImageService;

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody CreateBookingRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please sign in to book a hotel"));
        }
        try {
            HotelBooking booking = hotelBookingService.createBooking(userId, request);
            return ResponseEntity.ok(toDTO(booking));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<HotelBookingDTO>> getMyBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<HotelBooking> bookings = hotelBookingService.getByUser(userId);
        List<HotelBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/provider")
    public ResponseEntity<List<HotelBookingDTO>> getProviderBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<HotelBooking> bookings = hotelBookingService.getByProvider(userId);
        List<HotelBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/provider/revenue")
    public ResponseEntity<Map<String, Object>> getProviderRevenue(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        double revenue = hotelBookingService.getTotalRevenueForProvider(userId);
        int bookings = hotelBookingService.getBookingCountForProvider(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", revenue);
        data.put("totalBookings", bookings);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<HotelBookingDTO>> getAllBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<HotelBooking> bookings = hotelBookingService.getAll();
        List<HotelBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam BookingStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        HotelBooking updated = hotelBookingService.updateStatus(id, status);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelBookingDTO> getBooking(@PathVariable Long id) {
        HotelBooking booking = hotelBookingService.getById(id);
        if (booking == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(booking));
    }

    private HotelBookingDTO toDTO(HotelBooking booking) {
        List<String> imageUrls = hotelImageService.getImageUrls(booking.getHotel().getId());
        return HotelBookingDTO.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getName())
                .hotelId(booking.getHotel().getId())
                .hotelName(booking.getHotel().getName())
                .hotelLocation(booking.getHotel().getLocation())
                .hotelImageUrl(imageUrls.isEmpty() ? booking.getHotel().getImageUrl() : imageUrls.get(0))
                .roomTypeId(booking.getRoomType().getId())
                .roomTypeName(booking.getRoomType().getName())
                .roomCount(booking.getRoomCount())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .totalNights(booking.getTotalNights())
                .totalPrice(booking.getTotalPrice())
                .pricePerNight(booking.getPricePerNight())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .specialRequests(booking.getSpecialRequests())
                .build();
    }
}
