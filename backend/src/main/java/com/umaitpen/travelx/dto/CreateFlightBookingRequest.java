package com.umaitpen.travelx.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFlightBookingRequest {
    private Long flightId;
    private Long flightClassId;
    private Integer passengerCount;
    private LocalDate travelDate;
    private List<PassengerInfo> passengers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassengerInfo {
        private String passengerName;
        private String email;
        private String phone;
        private String gender;
        private Integer age;
        private String passengerType;
    }
}