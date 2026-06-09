package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.dto.DestinationRecommendationDTO;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.model.Expense;
import com.umaitpen.travelx.model.Booking;
import com.umaitpen.travelx.model.HotelBooking;
import com.umaitpen.travelx.model.FlightBooking;
import com.umaitpen.travelx.service.AIService;
import com.umaitpen.travelx.service.TripService;
import com.umaitpen.travelx.service.ExpenseService;
import com.umaitpen.travelx.service.BookingService;
import com.umaitpen.travelx.repository.BookingRepository;
import com.umaitpen.travelx.repository.HotelBookingRepository;
import com.umaitpen.travelx.repository.FlightBookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Month;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements com.umaitpen.travelx.service.RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final AIService aiService;
    private final TripService tripService;
    private final ExpenseService expenseService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final FlightBookingRepository flightBookingRepository;

    @Override
    public List<DestinationRecommendationDTO> getRecommendationsForUser(Long userId) {
        Map<String, Object> context = buildRecommendationContext(userId);
        return aiService.getDestinationRecommendations(context);
    }

    @Override
    public List<DestinationRecommendationDTO> getRecommendationsWithPreference(Long userId, String userPreference) {
        Map<String, Object> context = buildRecommendationContext(userId);
        context.put("userPreference", userPreference);
        return aiService.getDestinationRecommendations(context);
    }

    @Override
    public Map<String, Object> buildRecommendationContext(Long userId) {
        Map<String, Object> context = new HashMap<>();

        // Get user's trips for travel history
        List<Trip> userTrips = tripService.listByOwner(userId);
        String userHistory = buildUserHistorySummary(userTrips);
        context.put("userHistory", userHistory);

        // Analyze user's travel preferences
        String preferredBudget = analyzeBudgetPreference(userTrips);
        context.put("userBudget", preferredBudget);

        // Get current weather (simplified - in production, use a weather API)
        String currentWeather = getCurrentWeatherCondition();
        context.put("currentWeather", currentWeather);

        // Analyze climate preferences from past trips
        String preferredClimate = analyzeClimatePreference(userTrips);
        context.put("preferredClimate", preferredClimate);

        // User preference (can be empty for initial load)
        context.put("userPreference", "");

        return context;
    }

    private String buildUserHistorySummary(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return "First-time traveler - no previous trips recorded";
        }

        StringBuilder summary = new StringBuilder();
        Set<String> destinations = new HashSet<>();
        Set<String> tripTypes = new HashSet<>();

        for (Trip trip : trips) {
            if (trip.getDestination() != null) {
                destinations.add(trip.getDestination().toLowerCase());
                // Infer trip type from destination
                String dest = trip.getDestination().toLowerCase();
                if (containsAny(dest, "goa", "kerala", "beach", "coast", "marina")) {
                    tripTypes.add("Beach");
                }
                if (containsAny(dest, "manali", "shimla", "darjeeling", "ooty", "munnar", "hill", "mountain")) {
                    tripTypes.add("Hill Station");
                }
                if (containsAny(dest, "rajasthan", "jaipur", "udaipur", "jodhpur", "heritage")) {
                    tripTypes.add("Culture/Heritage");
                }
                if (containsAny(dest, "kerala", "wildlife", "bandipur", "jim corbett")) {
                    tripTypes.add("Wildlife/Nature");
                }
                if (containsAny(dest, "adventure", "trek", "rafting", "rishikesh")) {
                    tripTypes.add("Adventure");
                }
            }
        }

        if (!destinations.isEmpty()) {
            summary.append("Previously visited: ").append(String.join(", ", destinations));
        }
        if (!tripTypes.isEmpty()) {
            summary.append(". Preferred trip types: ").append(String.join(", ", tripTypes));
        }

        return summary.length() > 0 ? summary.toString() : "Regular traveler with varied interests";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String analyzeBudgetPreference(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return "moderate";
        }

        double avgBudget = trips.stream()
                .filter(t -> t.getBudget() != null)
                .mapToDouble(Trip::getBudget)
                .average()
                .orElse(10000);

        if (avgBudget < 5000) return "budget";
        if (avgBudget < 15000) return "moderate";
        if (avgBudget < 30000) return "mid-range";
        return "luxury";
    }

    private String analyzeClimatePreference(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return " Pleasant weather destinations";
        }

        // Analyze based on destinations visited
        StringBuilder preferences = new StringBuilder();
        for (Trip trip : trips) {
            if (trip.getDestination() != null) {
                String dest = trip.getDestination().toLowerCase();
                if (containsAny(dest, "manali", "shimla", "darjeeling", "ooty", "kashmir", "sikkim", "hill", "mountain")) {
                    preferences.append("Cold/Hill station destinations. ");
                }
                if (containsAny(dest, "goa", "kerala", "mumbai", "chennai", "coast", "beach")) {
                    preferences.append("Warm/Coastal destinations. ");
                }
            }
        }

        return preferences.length() > 0 ? preferences.toString().trim() : "All types";
    }

    private String getCurrentWeatherCondition() {
        // In production, integrate with a real weather API
        // For now, return current season-based weather
        Month month = java.time.LocalDate.now().getMonth();
        switch (month) {
            case DECEMBER:
            case JANUARY:
            case FEBRUARY:
                return "Winter - Cold temperatures across North India, Pleasant in South";
            case MARCH:
            case APRIL:
            case MAY:
                return "Spring - Pleasant weather, ideal for travel";
            case JUNE:
            case JULY:
            case AUGUST:
                return "Monsoon - Rainy in most regions, occasional sunshine";
            case SEPTEMBER:
            case OCTOBER:
            case NOVEMBER:
                return "Autumn - Pleasant weather, ideal for travel";
            default:
                return "Pleasant - Good for most destinations";
        }
    }
}
