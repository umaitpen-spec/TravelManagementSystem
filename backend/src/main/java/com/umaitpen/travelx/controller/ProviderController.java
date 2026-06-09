package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.FlightClass;
import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.RoomType;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.service.FlightClassService;
import com.umaitpen.travelx.service.FlightService;
import com.umaitpen.travelx.service.HotelService;
import com.umaitpen.travelx.service.RoomTypeService;
import com.umaitpen.travelx.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/provider")
@RequiredArgsConstructor
public class ProviderController {
    private final HotelService hotelService;
    private final FlightService flightService;
    private final FlightClassService flightClassService;
    private final UserService userService;
    private final RoomTypeService roomTypeService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        User user = userService.findById(userId);
        model.addAttribute("userName", user.getName());
        model.addAttribute("providerType", user.getProviderType());
        model.addAttribute("providerCompany", user.getProviderCompany());
        if (user.getProviderType() == ProviderType.HOTEL_PROVIDER) {
            model.addAttribute("hotels", hotelService.getByProvider(userId));
        } else if (user.getProviderType() == ProviderType.FLIGHT_PROVIDER) {
            model.addAttribute("flights", flightService.getByProvider(userId));
        }
        return "provider-dashboard";
    }

    // Hotel CRUD
    @GetMapping("/hotels/{id}")
    public ResponseEntity<?> getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.getById(id);
        if (hotel == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(hotel);
    }

    @PostMapping("/hotels")
    public ResponseEntity<?> createHotel(@RequestBody Hotel hotel, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Hotel created = hotelService.create(hotel, userId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody Hotel hotel) {
        Hotel updated = hotelService.update(id, hotel);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/hotels/{id}")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Hotel deleted"));
    }

    // Room Type CRUD
    @GetMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        List<RoomType> types = roomTypeService.getByHotel(hotelId);
        return ResponseEntity.ok(types);
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @RequestBody RoomType roomType, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Hotel hotel = hotelService.getById(hotelId);
        if (hotel == null || hotel.getProvider() == null || !hotel.getProvider().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        RoomType created = roomTypeService.create(roomType, hotelId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/hotels/{hotelId}/room-types/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long hotelId, @PathVariable Long id, @RequestBody RoomType roomType) {
        RoomType updated = roomTypeService.update(id, roomType);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/hotels/{hotelId}/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Room type deleted"));
    }

    // Flight CRUD
    @GetMapping("/flights/{id}")
    public ResponseEntity<?> getFlight(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Flight flight = flightService.getById(id);
        if (flight == null) return ResponseEntity.notFound().build();
        if (flight.getProvider() != null && !flight.getProvider().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        return ResponseEntity.ok(flight);
    }

    @PostMapping("/flights")
    public ResponseEntity<?> createFlight(@RequestBody Flight flight, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Flight created = flightService.create(flight, userId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/flights/{id}")
    public ResponseEntity<?> updateFlight(@PathVariable Long id, @RequestBody Flight flight) {
        Flight updated = flightService.update(id, flight);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/flights/{id}")
    public ResponseEntity<?> deleteFlight(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Flight deleted"));
    }

    // FlightClass CRUD
    @GetMapping("/flights/{flightId}/classes")
    public ResponseEntity<?> getFlightClasses(@PathVariable Long flightId) {
        List<FlightClass> classes = flightClassService.getByFlight(flightId);
        return ResponseEntity.ok(classes);
    }

    @PostMapping("/flights/{flightId}/classes")
    public ResponseEntity<?> createFlightClass(@PathVariable Long flightId, @RequestBody FlightClass flightClass, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Flight flight = flightService.getById(flightId);
        if (flight == null || flight.getProvider() == null || !flight.getProvider().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        FlightClass created = flightClassService.create(flightClass, flightId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/flights/{flightId}/classes/{id}")
    public ResponseEntity<?> updateFlightClass(@PathVariable Long flightId, @PathVariable Long id, @RequestBody FlightClass flightClass) {
        FlightClass updated = flightClassService.update(id, flightClass);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/flights/{flightId}/classes/{id}")
    public ResponseEntity<?> deleteFlightClass(@PathVariable Long id) {
        flightClassService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Flight class deleted"));
    }
}