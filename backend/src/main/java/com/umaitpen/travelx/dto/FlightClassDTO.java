package com.umaitpen.travelx.dto;

import com.umaitpen.travelx.enums.SeatClass;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightClassDTO {
    private Long id;
    private SeatClass classType;
    private Integer totalSeats;
    private Integer availableSeats;
    private Double price;
    private String baggageLimit;
    private Boolean refundable;
}
