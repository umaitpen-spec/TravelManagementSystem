package com.umaitpen.travelx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umaitpen.travelx.dto.TripRequestDTO;
import com.umaitpen.travelx.model.Itinerary;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.ItineraryRepository;
import com.umaitpen.travelx.service.AIService;
import com.umaitpen.travelx.service.TripService;
import com.umaitpen.travelx.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;
    private final UserService userService;
    private final AIService aiService;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TripRequestDTO request) {
        User owner = userService.findById(request.getOwnerId());
        if (owner == null) {
            return ResponseEntity.badRequest().body("Owner not found");
        }
        Trip trip = Trip.builder()
                .title(request.getTitle())
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .travelers(request.getTravelers())
                .budget(request.getBudget())
                .owner(owner)
                .build();
        Trip saved = tripService.createTrip(trip);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Trip trip = tripService.getTrip(id);
        if (trip == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Trip>> listByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(tripService.listByOwner(ownerId));
    }

    @GetMapping("/{id}/itinerary")
    public ResponseEntity<?> generateItinerary(@PathVariable Long id) {
        Trip trip = tripService.getTrip(id);
        if (trip == null) return ResponseEntity.notFound().build();
        String json = aiService.generateItineraryJson(trip);
        return ResponseEntity.ok(json);
    }

    @GetMapping("/{id}/itinerary/plans")
    public ResponseEntity<?> generateMultipleItineraryPlans(@PathVariable Long id,
            @RequestParam(name = "preferences", required = false) String preferences) {
        Trip trip = tripService.getTrip(id);
        if (trip == null) return ResponseEntity.notFound().build();

        int existingPlans = itineraryRepository.findByTripId(id).size();
        if (existingPlans >= aiService.getMaxPlansPerTrip()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maximum " + aiService.getMaxPlansPerTrip() + " plans already generated for this trip"
            ));
        }

        List<String> plans = aiService.generateMultipleItineraryPlans(trip, preferences);
        return ResponseEntity.ok(Map.of(
                "tripId", id,
                "maxPlans", aiService.getMaxPlansPerTrip(),
                "generatedPlans", plans.size(),
                "plans", plans
        ));
    }

    @GetMapping("/{id}/itinerary/plan/{planNumber}")
    public ResponseEntity<?> generateSingleItineraryPlan(@PathVariable Long id,
            @PathVariable int planNumber,
            @RequestParam(name = "preferences", required = false) String preferences) {
        Trip trip = tripService.getTrip(id);
        if (trip == null) return ResponseEntity.notFound().build();

        int existingPlans = itineraryRepository.findByTripId(id).size();
        if (existingPlans >= aiService.getMaxPlansPerTrip()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maximum " + aiService.getMaxPlansPerTrip() + " plans already generated for this trip"
            ));
        }

        if (planNumber < 1 || planNumber > aiService.getMaxPlansPerTrip()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Plan number must be between 1 and " + aiService.getMaxPlansPerTrip()
            ));
        }

        String plan = aiService.generateSingleItineraryPlan(trip, planNumber, preferences);
        return ResponseEntity.ok(Map.of(
                "tripId", id,
                "planNumber", planNumber,
                "maxPlans", aiService.getMaxPlansPerTrip(),
                "existingPlans", existingPlans,
                "plan", plan
        ));
    }

    @PostMapping("/{id}/itinerary/save")
    public ResponseEntity<?> saveItinerary(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Trip trip = tripService.getTrip(id);
        if (trip == null) return ResponseEntity.notFound().build();

        int existingPlans = itineraryRepository.findByTripId(id).size();
        if (existingPlans >= aiService.getMaxPlansPerTrip()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maximum " + aiService.getMaxPlansPerTrip() + " plans already saved for this trip"
            ));
        }

        try {
            // Store complete plan JSON (summary, budgetBreakdown, travelTips, itinerary, etc.)
            String planDetailsJson = objectMapper.writeValueAsString(request);
            String itineraryJson = objectMapper.writeValueAsString(request.get("itinerary"));
            Double estimatedCost = request.get("budget") != null ?
                    ((Number) request.get("budget")).doubleValue() :
                    (request.get("estimatedCost") != null ? ((Number) request.get("estimatedCost")).doubleValue() : 0.0);

            Itinerary itinerary = Itinerary.builder()
                    .trip(trip)
                    .dayLabel((String) request.getOrDefault("planLabel",
                            request.getOrDefault("dayLabel", "Plan " + (existingPlans + 1)).toString()))
                    .activitiesJson(itineraryJson)
                    .estimatedCost(estimatedCost)
                    .planDetailsJson(planDetailsJson)
                    .build();

            itineraryRepository.save(itinerary);

            return ResponseEntity.ok(Map.of(
                    "message", "Itinerary saved successfully",
                    "planNumber", existingPlans + 1
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to save itinerary: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/itineraries")
    public ResponseEntity<List<Itinerary>> getItineraries(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryRepository.findByTripId(id));
    }
}
