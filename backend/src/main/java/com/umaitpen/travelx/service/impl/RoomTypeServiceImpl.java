package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.RoomType;
import com.umaitpen.travelx.repository.HotelRepository;
import com.umaitpen.travelx.repository.RoomTypeRepository;
import com.umaitpen.travelx.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public RoomType create(RoomType roomType, Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) return null;
        roomType.setHotel(hotel);
        return roomTypeRepository.save(roomType);
    }

    @Override
    @Transactional
    public RoomType update(Long id, RoomType roomType) {
        RoomType existing = roomTypeRepository.findById(id).orElse(null);
        if (existing == null) return null;
        existing.setName(roomType.getName());
        existing.setPricePerNight(roomType.getPricePerNight());
        existing.setAvailableCount(roomType.getAvailableCount());
        existing.setCapacity(roomType.getCapacity());
        existing.setAc(roomType.isAc());
        existing.setFeatures(roomType.getFeatures());
        existing.setImageUrl(roomType.getImageUrl());
        return roomTypeRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        roomTypeRepository.deleteById(id);
    }

    @Override
    public List<RoomType> getByHotel(Long hotelId) {
        return roomTypeRepository.findByHotelId(hotelId);
    }

    @Override
    public RoomType getById(Long id) {
        return roomTypeRepository.findById(id).orElse(null);
    }

    @Override
    public boolean hasAvailability(Long roomTypeId, Integer requestedCount) {
        RoomType rt = roomTypeRepository.findById(roomTypeId).orElse(null);
        return rt != null && rt.getAvailableCount() != null && rt.getAvailableCount() >= requestedCount;
    }

    @Override
    @Transactional
    public void reduceAvailability(Long roomTypeId, Integer count) {
        RoomType rt = roomTypeRepository.findById(roomTypeId).orElse(null);
        if (rt != null) {
            rt.setAvailableCount(rt.getAvailableCount() - count);
            roomTypeRepository.save(rt);
        }
    }

    @Override
    @Transactional
    public void restoreAvailability(Long roomTypeId, Integer count) {
        RoomType rt = roomTypeRepository.findById(roomTypeId).orElse(null);
        if (rt != null) {
            rt.setAvailableCount(rt.getAvailableCount() + count);
            roomTypeRepository.save(rt);
        }
    }
}
