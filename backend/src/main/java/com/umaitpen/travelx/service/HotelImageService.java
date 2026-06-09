package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.HotelImage;

import java.util.List;

public interface HotelImageService {
    HotelImage create(HotelImage image, Long hotelId);
    void delete(Long id);
    List<HotelImage> getByHotel(Long hotelId);
    List<String> getImageUrls(Long hotelId);
}
