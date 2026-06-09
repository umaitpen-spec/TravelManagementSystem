package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.HotelRepository;
import com.umaitpen.travelx.repository.UserRepository;
import com.umaitpen.travelx.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    @Override
    public Hotel create(Hotel hotel, Long providerId) {
        User provider = userRepository.findById(providerId).orElse(null);
        if (provider != null) {
            hotel.setProvider(provider);
        }
        hotel.setActive(true);
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel update(Long id, Hotel hotel) {
        Hotel existing = hotelRepository.findById(id).orElse(null);
        if (existing == null) return null;
        existing.setName(hotel.getName());
        existing.setLocation(hotel.getLocation());
        existing.setAddress(hotel.getAddress());
        existing.setDescription(hotel.getDescription());
        existing.setAmenities(hotel.getAmenities());
        existing.setPricePerNight(hotel.getPricePerNight());
        existing.setRating(hotel.getRating());
        existing.setImageUrl(hotel.getImageUrl());
        existing.setActive(hotel.isActive());
        return hotelRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }

    @Override
    public List<Hotel> getByProvider(Long providerId) {
        return hotelRepository.findByProviderId(providerId);
    }

    @Override
    public List<Hotel> getAll() {
        return hotelRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Hotel getById(Long id) {
        return hotelRepository.findById(id).orElse(null);
    }

    @Override
    public List<Hotel> getByLocation(String location) {
        return hotelRepository.findAll().stream()
                .filter(h -> h.getLocation() != null && h.getLocation().equalsIgnoreCase(location))
                .toList();
    }

    @Override
    public Hotel updateActive(Long id, boolean active) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return null;
        hotel.setActive(active);
        return hotelRepository.save(hotel);
    }
}