package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.HotelDTO;
import com.umaitpen.travelx.dto.RoomTypeDTO;
import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.HotelImage;
import com.umaitpen.travelx.model.RoomType;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.HotelRepository;
import com.umaitpen.travelx.service.HotelImageService;
import com.umaitpen.travelx.service.HotelService;
import com.umaitpen.travelx.service.RoomTypeService;
import com.umaitpen.travelx.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;
    private final HotelImageService hotelImageService;
    private final HotelRepository hotelRepository;
    private final UserService userService;

    @GetMapping("/location/{location}")
    public ResponseEntity<List<HotelDTO>> searchByLocation(@PathVariable String location) {
        List<Hotel> hotels = hotelService.getByLocation(location);
        List<HotelDTO> dtos = hotels.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels() {
        List<Hotel> hotels = hotelService.getAll();
        List<HotelDTO> dtos = hotels.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDTO> getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.getById(id);
        if (hotel == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(hotel));
    }

    @GetMapping("/{id}/room-types")
    public ResponseEntity<List<RoomTypeDTO>> getRoomTypes(@PathVariable Long id) {
        List<RoomType> types = roomTypeService.getByHotel(id);
        List<RoomTypeDTO> dtos = types.stream().map(this::toRoomTypeDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Provider-side management
    @PostMapping
    public ResponseEntity<?> createHotel(@RequestBody Hotel hotel, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        Hotel created = hotelService.create(hotel, userId);
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody Hotel hotel) {
        Hotel updated = hotelService.update(id, hotel);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.ok("{\"message\": \"Hotel deleted\"}");
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id, @RequestParam boolean active) {
        Hotel updated = hotelService.updateActive(id, active);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    // Room type management
    @PostMapping("/{hotelId}/room-types")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @RequestBody RoomType roomType, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        RoomType created = roomTypeService.create(roomType, hotelId);
        if (created == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toRoomTypeDTO(created));
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @RequestBody RoomType roomType) {
        RoomType updated = roomTypeService.update(id, roomType);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toRoomTypeDTO(updated));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.ok("{\"message\": \"Room type deleted\"}");
    }

    // Image management
    @PostMapping("/{hotelId}/images")
    public ResponseEntity<?> addImage(@PathVariable Long hotelId, @RequestBody HotelImage image, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        HotelImage created = hotelImageService.create(image, hotelId);
        if (created == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        hotelImageService.delete(id);
        return ResponseEntity.ok("{\"message\": \"Image deleted\"}");
    }

    private HotelDTO toDTO(Hotel hotel) {
        List<String> imageUrls = hotelImageService.getImageUrls(hotel.getId());
        List<RoomTypeDTO> roomTypeDTOs = hotel.getRoomTypes().stream()
                .map(this::toRoomTypeDTO)
                .collect(Collectors.toList());

        return HotelDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .location(hotel.getLocation())
                .address(hotel.getAddress())
                .description(hotel.getDescription())
                .amenities(hotel.getAmenities())
                .rating(hotel.getRating())
                .imageUrl(hotel.getImageUrl())
                .pricePerNight(hotel.getPricePerNight())
                .providerId(hotel.getProvider() != null ? hotel.getProvider().getId() : null)
                .providerName(hotel.getProvider() != null ? hotel.getProvider().getName() : null)
                .totalAvailableRooms(hotel.getTotalAvailableRooms())
                .roomTypes(roomTypeDTOs)
                .imageUrls(imageUrls)
                .build();
    }

    private RoomTypeDTO toRoomTypeDTO(RoomType rt) {
        return RoomTypeDTO.builder()
                .id(rt.getId())
                .hotelId(rt.getHotel().getId())
                .hotelName(rt.getHotel().getName())
                .name(rt.getName())
                .pricePerNight(rt.getPricePerNight())
                .availableCount(rt.getAvailableCount())
                .capacity(rt.getCapacity())
                .ac(rt.isAc())
                .features(rt.getFeatures())
                .imageUrl(rt.getImageUrl())
                .build();
    }
}
