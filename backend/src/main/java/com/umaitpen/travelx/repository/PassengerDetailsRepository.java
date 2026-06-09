package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.model.PassengerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassengerDetailsRepository extends JpaRepository<PassengerDetails, Long> {
    List<PassengerDetails> findByFlightBookingId(Long flightBookingId);
}