package com.umaitpen.travelx.dto;

import com.umaitpen.travelx.enums.BookingStatus;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelBookingDTO {
    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private Long userId;
    private String userName;
    private Long hotelId;
    private String hotelName;
    private String hotelLocation;
    private String hotelImageUrl;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer roomCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer totalNights;
    private Double totalPrice;
    private Double pricePerNight;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String specialRequests;
}
