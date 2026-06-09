package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.CreateFlightBookingRequest;
import com.umaitpen.travelx.dto.FlightBookingDTO;
import com.umaitpen.travelx.dto.PassengerDTO;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.FlightBooking;
import com.umaitpen.travelx.model.PassengerDetails;
import com.umaitpen.travelx.repository.PassengerDetailsRepository;
import com.umaitpen.travelx.service.FlightBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flight-bookings")
@RequiredArgsConstructor
public class FlightBookingController {
    private final FlightBookingService flightBookingService;
    private final PassengerDetailsRepository passengerDetailsRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody CreateFlightBookingRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please sign in to book a flight"));
        }
        try {
            FlightBooking booking = flightBookingService.createBooking(userId, request);
            return ResponseEntity.ok(toDTO(booking));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<FlightBookingDTO>> getMyBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<FlightBooking> bookings = flightBookingService.getByUser(userId);
        List<FlightBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/provider")
    public ResponseEntity<List<FlightBookingDTO>> getProviderBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<FlightBooking> bookings = flightBookingService.getByProvider(userId);
        List<FlightBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/provider/revenue")
    public ResponseEntity<Map<String, Object>> getProviderRevenue(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        double revenue = flightBookingService.getTotalRevenueForProvider(userId);
        int bookings = flightBookingService.getBookingCountForProvider(userId);
        int seatsFilled = flightBookingService.getSeatsFilledForProvider(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", revenue);
        data.put("totalBookings", bookings);
        data.put("seatsFilled", seatsFilled);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<FlightBookingDTO>> getAllBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<FlightBooking> bookings = flightBookingService.getAll();
        List<FlightBookingDTO> dtos = bookings.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam BookingStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        FlightBooking updated = flightBookingService.updateStatus(id, status);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightBookingDTO> getBooking(@PathVariable Long id) {
        FlightBooking booking = flightBookingService.getById(id);
        if (booking == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(booking));
    }

    private FlightBookingDTO toDTO(FlightBooking booking) {
        List<PassengerDetails> passengers = passengerDetailsRepository.findByFlightBookingId(booking.getId());
        List<PassengerDTO> passengerDTOs = passengers.stream()
                .map(p -> PassengerDTO.builder()
                        .id(p.getId())
                        .passengerName(p.getPassengerName())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .seatNumber(p.getSeatNumber())
                        .passengerType(p.getPassengerType())
                        .build())
                .collect(Collectors.toList());

        return FlightBookingDTO.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getName())
                .flightId(booking.getFlight().getId())
                .airline(booking.getFlight().getAirline())
                .flightNumber(booking.getFlight().getFlightNumber())
                .origin(booking.getFlight().getOrigin())
                .destination(booking.getFlight().getDestination())
                .sourceCity(booking.getFlight().getSourceCity())
                .destinationCity(booking.getFlight().getDestinationCity())
                .travelDate(booking.getTravelDate())
                .bookingDate(booking.getCreatedAt() != null ? booking.getCreatedAt().toLocalDate() : null)
                .flightClassId(booking.getFlightClass().getId())
                .classType(booking.getFlightClass().getClassType().name())
                .passengerCount(booking.getPassengerCount())
                .totalPrice(booking.getTotalPrice())
                .pricePerTicket(booking.getPricePerTicket())
                .airlineLogo(booking.getFlight().getAirlineLogo())
                .passengers(passengerDTOs)
                .build();
    }
}