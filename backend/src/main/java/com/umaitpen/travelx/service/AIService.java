package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.dto.DestinationRecommendationDTO;
import java.util.List;
import java.util.Map;

public interface AIService {
    String generateItineraryJson(Trip trip);
    List<String> generateMultipleItineraryPlans(Trip trip, String preferences);
    String generateSingleItineraryPlan(Trip trip, int planNumber, String preferences);
    int getMaxPlansPerTrip();
    List<DestinationRecommendationDTO> getDestinationRecommendations(Map<String, Object> context);
}
