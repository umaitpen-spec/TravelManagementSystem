package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByProviderId(Long providerId);
    List<Hotel> findAllByOrderByCreatedAtDesc();
    List<Hotel> findByLocationContainingIgnoreCaseAndActiveTrue(String location);
    List<Hotel> findByActiveTrueOrderByCreatedAtDesc();
}
