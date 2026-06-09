package com.umaitpen.travelx.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "passenger_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "flight_booking_id", nullable = false)
    private FlightBooking flightBooking;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String email;

    private String phone;
    private String gender;
    private Integer age;
    private String seatNumber;
    private String passengerType;
}