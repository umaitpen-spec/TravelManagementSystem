package com.umaitpen.travelx.dto;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.enums.FlightType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightDTO {
    private Long id;
    private String airline;
    private String flightNumber;
    private String origin;
    private String destination;
    private String sourceCity;
    private String destinationCity;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String duration;
    private FlightType flightType;
    private String baggageDetails;
    private String description;
    private String imageUrl;
    private String airlineLogo;
    private String amenities;
    private FlightStatus status;
    private String cancellationPolicy;
    private Long providerId;
    private String providerName;
    private Boolean isFull;
    private List<FlightClassDTO> flightClasses;
}