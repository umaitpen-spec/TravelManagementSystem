package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeDTO {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String name;
    private Double pricePerNight;
    private Integer availableCount;
    private Integer capacity;
    private boolean ac;
    private String features;
    private String imageUrl;
}
