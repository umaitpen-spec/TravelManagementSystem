package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.FlightBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightBookingRepository extends JpaRepository<FlightBooking, Long> {
    List<FlightBooking> findByUserId(Long userId);
    List<FlightBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<FlightBooking> findByFlightProviderId(Long providerId);
    List<FlightBooking> findByFlightProviderIdOrderByCreatedAtDesc(Long providerId);
    List<FlightBooking> findByFlightId(Long flightId);
    List<FlightBooking> findByStatus(BookingStatus status);
    List<FlightBooking> findByBookingReference(String bookingReference);
}