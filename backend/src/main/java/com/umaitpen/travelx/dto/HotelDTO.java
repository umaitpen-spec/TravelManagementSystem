package com.umaitpen.travelx.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelDTO {
    private Long id;
    private String name;
    private String location;
    private String address;
    private String description;
    private String amenities;
    private Double rating;
    private String imageUrl;
    private Double pricePerNight;
    private Long providerId;
    private String providerName;
    private int totalAvailableRooms;
    private List<RoomTypeDTO> roomTypes;
    private List<String> imageUrls;
}
