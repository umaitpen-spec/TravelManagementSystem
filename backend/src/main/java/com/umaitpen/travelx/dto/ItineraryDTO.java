package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDTO {
    private String dayLabel;
    private String activitiesJson;
    private Double estimatedCost;
}
