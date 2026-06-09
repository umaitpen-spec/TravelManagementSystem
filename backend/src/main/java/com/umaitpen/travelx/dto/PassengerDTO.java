package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerDTO {
    private Long id;
    private String passengerName;
    private String email;
    private String phone;
    private String gender;
    private Integer age;
    private String seatNumber;
    private String passengerType;
}