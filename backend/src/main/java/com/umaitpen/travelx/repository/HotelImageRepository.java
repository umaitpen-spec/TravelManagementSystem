package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.model.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findByHotelId(Long hotelId);
    List<HotelImage> findByHotelIdOrderByPrimaryImageDesc(Long hotelId);
}
