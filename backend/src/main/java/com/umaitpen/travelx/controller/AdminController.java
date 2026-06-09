package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final TripService tripService;
    private final HotelService hotelService;
    private final FlightService flightService;
    private final BookingService bookingService;
    private final ExpenseService expenseService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        User user = userService.findById(userId);
        if (user.getRole() != Role.ROLE_ADMIN) return "redirect:/";

        model.addAttribute("userName", user.getName());
        model.addAttribute("totalUsers", userService.countByRole(Role.ROLE_USER));
        model.addAttribute("totalProviders", userService.countByRole(Role.ROLE_SERVICE_PROVIDER));
        model.addAttribute("providers", userService.findByRole(Role.ROLE_SERVICE_PROVIDER));
        model.addAttribute("allUsers", userService.findAll());
        model.addAttribute("allTrips", tripService.findAll()); // admin sees all trips
        model.addAttribute("hotels", hotelService.getAll());
        model.addAttribute("flights", flightService.getAll());
        return "admin-dashboard";
    }

    @PostMapping("/users/{id}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) return ResponseEntity.notFound().build();
        user.setEnabled(!user.isEnabled());
        userService.register(user);
        return ResponseEntity.ok(Map.of("enabled", user.isEnabled()));
    }
}