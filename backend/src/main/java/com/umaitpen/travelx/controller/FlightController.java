package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.FlightClassDTO;
import com.umaitpen.travelx.dto.FlightDTO;
import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.FlightClass;
import com.umaitpen.travelx.repository.FlightRepository;
import com.umaitpen.travelx.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {
    private final FlightService flightService;
    private final FlightRepository flightRepository;

    @GetMapping
    public ResponseEntity<List<FlightDTO>> getAllFlights() {
        List<Flight> flights = flightService.getAll();
        List<FlightDTO> dtos = flights.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<FlightDTO>> getActiveFlights() {
        List<Flight> flights = flightService.getActiveFlights();
        List<FlightDTO> dtos = flights.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable Long id) {
        Flight flight = flightService.getById(id);
        if (flight == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(flight));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FlightDTO>> searchFlights(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate) {
        List<Flight> flights;
        if (source != null || destination != null || departureDate != null) {
            flights = flightService.searchFlights(source, destination, departureDate);
        } else {
            flights = flightService.getActiveFlights();
        }
        List<FlightDTO> dtos = flights.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/origin/{origin}")
    public ResponseEntity<List<FlightDTO>> getByOrigin(@PathVariable String origin) {
        List<Flight> flights = flightService.getByOrigin(origin);
        List<FlightDTO> dtos = flights.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/destination/{destination}")
    public ResponseEntity<List<FlightDTO>> getByDestination(@PathVariable String destination) {
        List<Flight> flights = flightService.getByDestination(destination);
        List<FlightDTO> dtos = flights.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Provider-side management
    @PostMapping
    public ResponseEntity<?> createFlight(@RequestBody Flight flight, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        Flight created = flightService.create(flight, userId);
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFlight(@PathVariable Long id, @RequestBody Flight flight) {
        Flight updated = flightService.update(id, flight);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFlight(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.ok("{\"message\": \"Flight deleted\"}");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam FlightStatus status) {
        Flight updated = flightService.updateStatus(id, status);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    private FlightDTO toDTO(Flight flight) {
        List<FlightClassDTO> classDTOs = Collections.emptyList();
        if (flight.getFlightClasses() != null && !flight.getFlightClasses().isEmpty()) {
            classDTOs = flight.getFlightClasses().stream().map(this::classToDTO).collect(Collectors.toList());
        }
        int totalAvailable = classDTOs.stream().mapToInt(FlightClassDTO::getAvailableSeats).sum();
        return FlightDTO.builder()
                .id(flight.getId())
                .airline(flight.getAirline())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .sourceCity(flight.getSourceCity())
                .destinationCity(flight.getDestinationCity())
                .departureDate(flight.getDepartureDate())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .duration(flight.getDuration())
                .flightType(flight.getFlightType())
                .baggageDetails(flight.getBaggageDetails())
                .description(flight.getDescription())
                .imageUrl(flight.getImageUrl())
                .airlineLogo(flight.getAirlineLogo())
                .amenities(flight.getAmenities())
                .status(flight.getStatus())
                .cancellationPolicy(flight.getCancellationPolicy())
                .providerId(flight.getProvider() != null ? flight.getProvider().getId() : null)
                .providerName(flight.getProvider() != null ? flight.getProvider().getName() : null)
                .isFull(classDTOs.isEmpty() || classDTOs.stream().allMatch(c -> c.getAvailableSeats() <= 0))
                .flightClasses(classDTOs)
                .build();
    }

    private FlightClassDTO classToDTO(FlightClass fc) {
        return FlightClassDTO.builder()
                .id(fc.getId())
                .classType(fc.getClassType())
                .totalSeats(fc.getTotalSeats())
                .availableSeats(fc.getAvailableSeats())
                .price(fc.getPrice())
                .baggageLimit(fc.getBaggageLimit())
                .refundable(fc.isRefundable())
                .build();
    }
}