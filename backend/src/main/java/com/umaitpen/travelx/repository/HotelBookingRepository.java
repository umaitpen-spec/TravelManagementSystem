package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {
    List<HotelBooking> findByUserId(Long userId);
    List<HotelBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<HotelBooking> findByHotelProviderId(Long providerId);
    List<HotelBooking> findByHotelProviderIdOrderByCreatedAtDesc(Long providerId);
    List<HotelBooking> findByHotelId(Long hotelId);
    List<HotelBooking> findByStatus(BookingStatus status);
    List<HotelBooking> findByRoomTypeId(Long roomTypeId);
}
