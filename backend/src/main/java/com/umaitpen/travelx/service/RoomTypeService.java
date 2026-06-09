package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.RoomType;

import java.util.List;

public interface RoomTypeService {
    RoomType create(RoomType roomType, Long hotelId);
    RoomType update(Long id, RoomType roomType);
    void delete(Long id);
    List<RoomType> getByHotel(Long hotelId);
    RoomType getById(Long id);
    boolean hasAvailability(Long roomTypeId, Integer requestedCount);
    void reduceAvailability(Long roomTypeId, Integer count);
    void restoreAvailability(Long roomTypeId, Integer count);
}
