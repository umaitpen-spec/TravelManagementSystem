package com.umaitpen.travelx.service;

import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.User;

import java.util.List;

public interface HotelService {
    Hotel create(Hotel hotel, Long providerId);
    Hotel update(Long id, Hotel hotel);
    void delete(Long id);
    List<Hotel> getByProvider(Long providerId);
    List<Hotel> getAll();
    Hotel getById(Long id);
    List<Hotel> getByLocation(String location);
    Hotel updateActive(Long id, boolean active);
}