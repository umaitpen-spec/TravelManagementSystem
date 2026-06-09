package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.HotelImage;
import com.umaitpen.travelx.repository.HotelImageRepository;
import com.umaitpen.travelx.repository.HotelRepository;
import com.umaitpen.travelx.service.HotelImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelImageServiceImpl implements HotelImageService {
    private final HotelImageRepository hotelImageRepository;
    private final HotelRepository hotelRepository;

    @Override
    public HotelImage create(HotelImage image, Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) return null;
        image.setHotel(hotel);
        return hotelImageRepository.save(image);
    }

    @Override
    public void delete(Long id) {
        hotelImageRepository.deleteById(id);
    }

    @Override
    public List<HotelImage> getByHotel(Long hotelId) {
        return hotelImageRepository.findByHotelIdOrderByPrimaryImageDesc(hotelId);
    }

    @Override
    public List<String> getImageUrls(Long hotelId) {
        return hotelImageRepository.findByHotelIdOrderByPrimaryImageDesc(hotelId)
                .stream()
                .map(HotelImage::getImageUrl)
                .collect(Collectors.toList());
    }
}
