package com.umaitpen.travelx.service;

import com.umaitpen.travelx.dto.DestinationRecommendationDTO;
import java.util.List;
import java.util.Map;

public interface RecommendationService {
    List<DestinationRecommendationDTO> getRecommendationsForUser(Long userId);
    List<DestinationRecommendationDTO> getRecommendationsWithPreference(Long userId, String userPreference);
    Map<String, Object> buildRecommendationContext(Long userId);
}
