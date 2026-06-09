package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationRecommendationDTO {
    private String destination;
    private String description;
    private String currentWeather;
    private String weatherIcon;
    private String estimatedBudgetMin;
    private String estimatedBudgetMax;
    private String bestTimeToVisit;
    private String recommendationReason;
    private String[] suggestedActivities;
    private String[] tags;
    private String imageUrl;
}
