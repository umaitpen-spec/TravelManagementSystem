package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestDTO {
    private Long userId;
    private String userPreference;
    private String preferredBudget;
    private String preferredClimate;
}
