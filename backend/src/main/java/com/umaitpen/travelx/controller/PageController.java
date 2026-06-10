package com.umaitpen.travelx.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.Itinerary;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.enums.AuthProvider;
import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.ItineraryRepository;
import com.umaitpen.travelx.repository.UserRepository;
import com.umaitpen.travelx.service.AIService;
import com.umaitpen.travelx.service.BookingService;
import com.umaitpen.travelx.service.ExpenseService;
import com.umaitpen.travelx.service.FlightService;
import com.umaitpen.travelx.service.HotelService;
import com.umaitpen.travelx.service.TripService;
import com.umaitpen.travelx.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class PageController {
    private final TripService tripService;
    private final UserService userService;
    private final BookingService bookingService;
    private final ExpenseService expenseService;
    private final AIService aiService;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final HotelService hotelService;
    private final FlightService flightService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long requireLogin(HttpSession session, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long ownerId = null;

        System.out.println("=== requireLogin called ===");
        System.out.println("Auth type: " + (auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null"));

        // Check session first
        ownerId = (Long) session.getAttribute("userId");
        if (ownerId != null) {
            System.out.println("=== Found userId from session: " + ownerId);
            return ownerId;
        }

        // Check SecurityContext principal
        if (auth != null && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                ownerId = (Long) principal;
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                // Get userId from attributes (if set by custom service)
                Object userIdAttr = oauth2User.getAttributes().get("userId");
                if (userIdAttr != null) {
                    ownerId = ((Number) userIdAttr).longValue();
                } else {
                    // Fallback: get email and look up/create user
                    String email = (String) oauth2User.getAttributes().get("email");
                    String googleId = (String) oauth2User.getAttributes().get("sub");
                    String name = (String) oauth2User.getAttributes().get("name");
                    System.out.println("=== OAuth attrs - email: " + email + ", googleId: " + googleId);
                    if (email != null) {
                        try {
                            java.util.Optional<User> userOpt = userRepository.findByEmail(email);
                            if (userOpt.isPresent()) {
                                ownerId = userOpt.get().getId();
                                System.out.println("=== Found user by email: " + ownerId);
                            } else if (googleId != null) {
                                userOpt = userRepository.findByProviderId(googleId);
                                if (userOpt.isPresent()) {
                                    ownerId = userOpt.get().getId();
                                    System.out.println("=== Found user by providerId: " + ownerId);
                                }
                            }
                            // Create if still not found
                            if (ownerId == null) {
                                System.out.println("=== Creating user in requireLogin ===");
                                User newUser = User.builder()
                                    .email(email)
                                    .name(name != null ? name : email.split("@")[0])
                                    .password("")
                                    .role(Role.ROLE_USER)
                                    .authProvider(AuthProvider.GOOGLE)
                                    .providerId(googleId != null ? googleId : "")
                                    .enabled(true)
                                    .build();
                                newUser = userRepository.save(newUser);
                                ownerId = newUser.getId();
                                // Also set session
                                session.setAttribute("userId", ownerId);
                                session.setAttribute("userEmail", email);
                                session.setAttribute("userName", name);
                                System.out.println("=== Created user in requireLogin: " + ownerId);
                            }
                        } catch (Exception e) {
                            System.out.println("=== Error in requireLogin: " + e.getMessage());
                        }
                    }
                }
            }
        }

        // Final check - session attribute
        if (ownerId == null) {
            ownerId = (Long) session.getAttribute("userId");
        }

        if (ownerId == null) {
            System.out.println("=== requireLogin returning null ===");
            redirectAttributes.addFlashAttribute("loginError", "Please sign in first.");
        } else {
            // Ensure session has userId
            session.setAttribute("userId", ownerId);
        }
        return ownerId;
    }

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        System.out.println("=== / dashboard called ===");
        System.out.println("Session userId: " + session.getAttribute("userId"));
        System.out.println("Session id: " + session.getId());

        // If session.userId is null but SecurityContext has userId, populate session
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (session.getAttribute("userId") == null && auth != null && auth.getPrincipal() != null && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            session.setAttribute("userId", userId);
            System.out.println("=== Populated session from SecurityContext: userId=" + userId);
        }

        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) {
            return "redirect:/login";
        }

        Object userName = session.getAttribute("userName");
        model.addAttribute("userName", userName);

        List<Trip> trips = tripService.listByOwner(ownerId);
        model.addAttribute("trips", trips);
        if (!trips.isEmpty()) {
            model.addAttribute("activeItineraries", itineraryRepository.findByTripId(trips.get(0).getId()));
        }

        // Aggregate budget health across ALL trips
        double totalBudgetAll = 0;
        double totalSpentAll = 0;
        for (Trip t : trips) {
            totalBudgetAll += t.getBudget();
            totalSpentAll += expenseService.getExpensesForTrip(t.getId()).stream().mapToDouble(e -> e.getAmount()).sum();
        }
        double remainingAll = totalBudgetAll - totalSpentAll;
        String aggStatus;
        if (totalSpentAll == 0) {
            aggStatus = "NO_EXPENSES";
        } else if (remainingAll < 0) {
            aggStatus = "OVER_BUDGET";
        } else if (remainingAll <= totalBudgetAll * 0.15) {
            aggStatus = "OPTIMAL";
        } else {
            aggStatus = "UNDER_BUDGET";
        }
        model.addAttribute("budgetAnalysis",
            com.umaitpen.travelx.dto.BudgetAnalysisDTO.builder()
                .totalBudget(totalBudgetAll)
                .estimatedSpend(totalSpentAll)
                .remainingBudget(remainingAll)
                .status(aggStatus)
                .build());

        return "dashboard";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        System.out.println("=== /login page ===");
        System.out.println("Session userId: " + session.getAttribute("userId"));
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/trips/new")
    public String newTrip(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "trip-create";
    }

    @PostMapping("/trips")
    public String createTrip(
            @RequestParam String title,
            @RequestParam String destination,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Integer travelers,
            @RequestParam Double budget,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long ownerId = (Long) session.getAttribute("userId");
        if (ownerId == null) {
            redirectAttributes.addFlashAttribute("loginError", "Please sign in first.");
            return "redirect:/login";
        }
        User owner = userService.findById(ownerId);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("loginError", "User session invalid. Please sign in again.");
            return "redirect:/login";
        }
        Trip trip = Trip.builder()
                .title(title)
                .destination(destination)
                .startDate(startDate)
                .endDate(endDate)
                .travelers(travelers)
                .budget(budget)
                .owner(owner)
                .build();
        tripService.createTrip(trip);
        redirectAttributes.addFlashAttribute("planSuccess", "Trip plan created successfully.");
        return "redirect:/";
    }

    @GetMapping("/trips/{id}")
    public String tripDetails(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        Trip trip = tripService.getTrip(id);
        if (trip == null || !trip.getOwner().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("loginError", "Trip not found or access denied.");
            return "redirect:/";
        }
        model.addAttribute("trip", trip);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("bookings", bookingService.getBookingsForTrip(id));

        List<Itinerary> savedItineraries = itineraryRepository.findByTripId(id);
        model.addAttribute("savedItineraries", savedItineraries);

        // Pre-parse activitiesJson and planDetailsJson so HTML can display full saved itinerary
        List<Map<String, Object>> parsedItineraries = savedItineraries.stream().map(it -> {
            Map<String, Object> map = new HashMap<>();
            map.put("dayLabel", it.getDayLabel());
            map.put("estimatedCost", it.getEstimatedCost());
            try {
                List<Map<String, Object>> activities = objectMapper.readValue(
                    it.getActivitiesJson(), new TypeReference<List<Map<String, Object>>>() {});
                map.put("activities", activities);
            } catch (Exception e) {
                map.put("activities", Collections.emptyList());
            }
            // Parse and include full plan details (summary, budgetBreakdown, travelTips, etc.)
            try {
                if (it.getPlanDetailsJson() != null && !it.getPlanDetailsJson().isBlank()) {
                    Map<String, Object> fullPlan = objectMapper.readValue(
                        it.getPlanDetailsJson(), new TypeReference<Map<String, Object>>() {});
                    // Include all plan details
                    map.put("planDetails", fullPlan);
                    map.put("summary", fullPlan.get("summary"));
                    map.put("budgetBreakdown", fullPlan.get("budgetBreakdown"));
                    map.put("travelTips", fullPlan.get("travelTips"));
                    map.put("destination", fullPlan.get("destination"));
                    map.put("durationDays", fullPlan.get("durationDays"));
                    map.put("travelers", fullPlan.get("travelers"));
                    map.put("budget", fullPlan.get("budget"));
                    map.put("planId", fullPlan.get("planId"));
                    map.put("planLabel", fullPlan.get("planLabel"));
                }
            } catch (Exception e) {
                // Fallback: continue without full plan details
            }
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("parsedItineraries", parsedItineraries);

        return "trip-details";
    }

    @PostMapping("/trips/{id}/itinerary/save")
    public String saveItinerary(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        Trip trip = tripService.getTrip(id);
        if (trip == null || !trip.getOwner().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("loginError", "Trip not found or access denied.");
            return "redirect:/";
        }

        int existingPlans = itineraryRepository.findByTripId(id).size();
        if (existingPlans >= aiService.getMaxPlansPerTrip()) {
            redirectAttributes.addFlashAttribute("msg", "Maximum " + aiService.getMaxPlansPerTrip() + " plans already saved for this trip.");
            return "redirect:/trips/" + id;
        }

        try {
            String itineraryJson = aiService.generateItineraryJson(trip);
            Map<String, Object> parsed = objectMapper.readValue(itineraryJson, new TypeReference<Map<String, Object>>() {});

            String itineraryData = objectMapper.writeValueAsString(parsed.get("itinerary"));
            String dayLabel = (String) parsed.getOrDefault("planLabel", "Plan " + (existingPlans + 1));
            Double estimatedCost = parsed.get("budget") != null ? ((Number) parsed.get("budget")).doubleValue() : trip.getBudget();

            // Store complete plan JSON including summary, budgetBreakdown, travelTips, etc.
            String planDetailsJson = objectMapper.writeValueAsString(parsed);

            Itinerary itinerary = Itinerary.builder()
                    .trip(trip)
                    .dayLabel(dayLabel)
                    .activitiesJson(itineraryData)
                    .estimatedCost(estimatedCost)
                    .planDetailsJson(planDetailsJson)
                    .build();
            itineraryRepository.save(itinerary);

            redirectAttributes.addFlashAttribute("msg", "Itinerary saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "Failed to save itinerary: " + e.getMessage());
        }

        return "redirect:/trips/" + id;
    }

    @GetMapping("/bookings")
    public String bookings(
            @RequestParam(name = "tripId", required = false) Long tripId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("trips", tripService.listByOwner(ownerId));

        // Load service providers by type for the provider dropdown
        List<User> hotelProviders = userService.findByRole(Role.ROLE_SERVICE_PROVIDER).stream()
                .filter(u -> u.getProviderType() == ProviderType.HOTEL_PROVIDER)
                .collect(Collectors.toList());
        List<User> flightProviders = userService.findByRole(Role.ROLE_SERVICE_PROVIDER).stream()
                .filter(u -> u.getProviderType() == ProviderType.FLIGHT_PROVIDER)
                .collect(Collectors.toList());
        model.addAttribute("hotelProviders", hotelProviders);
        model.addAttribute("flightProviders", flightProviders);

        if (tripId != null) {
            Trip trip = tripService.getTrip(tripId);
            model.addAttribute("trip", trip);
        }
        return "booking";
    }

    @PostMapping("/trips/{id}/bookings")
    public String addBookingForm(@PathVariable Long id,
                                 @RequestParam String provider,
                                 @RequestParam String bookingType,
                                 @RequestParam String reference,
                                 @RequestParam Double price,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        Trip trip = tripService.getTrip(id);
        if (trip == null || !trip.getOwner().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("loginError", "Trip not found or access denied.");
            return "redirect:/";
        }
        com.umaitpen.travelx.model.Booking booking = com.umaitpen.travelx.model.Booking.builder()
                .provider(provider)
                .bookingType(bookingType)
                .reference(reference)
                .price(price)
                .build();
        bookingService.addBooking(id, booking);
        redirectAttributes.addFlashAttribute("msg", "Booking added.");
        return "redirect:/trips/" + id;
    }

    @GetMapping("/expenses")
    public String expenses(
            @RequestParam(name = "tripId", required = false) Long tripId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("trips", tripService.listByOwner(ownerId));

        if (tripId != null) {
            Trip trip = tripService.getTrip(tripId);
            model.addAttribute("trip", trip);

            model.addAttribute("expenses", expenseService.getExpensesForTrip(tripId));
            model.addAttribute("analysis", expenseService.analyzeBudget(tripId));
        }
        return "expenses";
    }

    @GetMapping("/hotels")
    public String hotelSearch(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "hotel-search";
    }

    @GetMapping("/my-trips")
    public String myTrips(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("trips", tripService.listByOwner(ownerId));
        if (!model.containsAttribute("trips")) {
            List<Trip> trips = tripService.listByOwner(ownerId);
            model.addAttribute("trips", trips);
        }
        return "dashboard";
    }

    @GetMapping("/my-bookings")
    public String myBookings(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "my-bookings";
    }

    @GetMapping("/book-hotel/{hotelId}")
    public String bookHotel(@PathVariable Long hotelId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        Hotel hotel = hotelService.getById(hotelId);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("msg", "Hotel not found.");
            return "redirect:/hotels";
        }
        model.addAttribute("hotel", hotel);
        model.addAttribute("roomTypes", hotel.getRoomTypes());
        return "hotel-booking";
    }

    @GetMapping("/flights/search")
    public String flightSearch(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "flight-search";
    }

    @GetMapping("/book-flight/{flightId}")
    public String bookFlight(@PathVariable Long flightId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        Flight flight = flightService.getFlight(flightId);
        if (flight == null) {
            redirectAttributes.addFlashAttribute("msg", "Flight not found.");
            return "redirect:/flights/search";
        }
        model.addAttribute("flight", flight);
        return "flight-booking";
    }

    @GetMapping("/my-flight-bookings")
    public String myFlightBookings(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "my-flight-bookings";
    }

    @GetMapping("/provider/hotels/new")
    public String newHotel(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "hotel-form";
    }

    @GetMapping("/provider/hotels/{id}/edit")
    public String editHotel(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        Hotel hotel = hotelService.getById(id);
        if (hotel == null || hotel.getProvider() == null || !hotel.getProvider().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("msg", "Hotel not found or access denied.");
            return "redirect:/provider/dashboard";
        }
        model.addAttribute("hotel", hotel);
        return "hotel-form";
    }

    @GetMapping("/provider/flights/new")
    public String newFlight(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        return "flight-add";
    }

    @GetMapping("/provider/flights/{id}/edit")
    public String editFlight(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute("userName"));
        Flight flight = flightService.getById(id);
        if (flight == null || flight.getProvider() == null || !flight.getProvider().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("msg", "Flight not found or access denied.");
            return "redirect:/provider/dashboard";
        }
        model.addAttribute("flight", flight);
        return "flight-edit";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess(HttpSession session, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("logoutSuccess", true);
        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/trips/{id}/expenses")
    public String addExpenseForm(
            @PathVariable Long id,
            @RequestParam String category,
            @RequestParam Double amount,
            @RequestParam String description,
            @RequestParam("expenseDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireLogin(session, redirectAttributes);
        if (ownerId == null) return "redirect:/login";

        Trip trip = tripService.getTrip(id);

        if (trip == null || !trip.getOwner().getId().equals(ownerId)) {
            redirectAttributes.addFlashAttribute("loginError", "Trip not found or access denied.");
            return "redirect:/";
        }

        if (expenseDate == null || expenseDate.isBefore(trip.getStartDate()) || expenseDate.isAfter(trip.getEndDate())) {
            redirectAttributes.addFlashAttribute("msg", "Expense date must be within the selected trip dates.");
            return "redirect:/expenses?tripId=" + id;
        }

        com.umaitpen.travelx.model.Expense expense = new com.umaitpen.travelx.model.Expense();
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setDescription(description);
        expense.setTrip(trip);
        expense.setExpenseDate(expenseDate);

        expenseService.addExpense(id, expense);

        redirectAttributes.addFlashAttribute("msg", "Expense recorded.");
        return "redirect:/expenses?tripId=" + id;
    }
}
