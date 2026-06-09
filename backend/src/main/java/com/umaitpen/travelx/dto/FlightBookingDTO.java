package com.umaitpen.travelx.dto;

import com.umaitpen.travelx.enums.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightBookingDTO {
    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private Long userId;
    private String userName;
    private Long flightId;
    private String airline;
    private String flightNumber;
    private String origin;
    private String destination;
    private String sourceCity;
    private String destinationCity;
    private LocalDate travelDate;
    private LocalDate bookingDate;
    private Long flightClassId;
    private String classType;
    private Integer passengerCount;
    private Double totalPrice;
    private Double pricePerTicket;
    private String airlineLogo;
    private List<PassengerDTO> passengers;
}