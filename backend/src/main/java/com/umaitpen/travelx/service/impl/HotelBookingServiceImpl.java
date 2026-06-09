package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.dto.CreateBookingRequest;
import com.umaitpen.travelx.dto.HotelBookingDTO;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.*;
import com.umaitpen.travelx.repository.*;
import com.umaitpen.travelx.service.HotelBookingService;
import com.umaitpen.travelx.service.HotelImageService;
import com.umaitpen.travelx.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelBookingServiceImpl implements HotelBookingService {
    private final HotelBookingRepository hotelBookingRepository;
    private final RoomTypeService roomTypeService;
    private final HotelImageService hotelImageService;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public HotelBooking createBooking(Long userId, CreateBookingRequest request) {
        // Validate availability
        if (!roomTypeService.hasAvailability(request.getRoomTypeId(), request.getRoomCount())) {
            throw new IllegalStateException("Not enough rooms available for selected room type");
        }

        // Validate dates
        if (request.getCheckInDate().isAfter(request.getCheckOutDate()) ||
            request.getCheckOutDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid check-in or check-out dates");
        }

        User user = userRepository.findById(userId).orElse(null);
        Hotel hotel = hotelRepository.findById(request.getHotelId()).orElse(null);
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId()).orElse(null);

        if (user == null || hotel == null || roomType == null) {
            throw new IllegalArgumentException("Invalid user, hotel, or room type");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        double totalPrice = nights * roomType.getPricePerNight() * request.getRoomCount();

        HotelBooking booking = HotelBooking.builder()
                .bookingReference("HB" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(BookingStatus.CONFIRMED)
                .user(user)
                .hotel(hotel)
                .roomType(roomType)
                .roomCount(request.getRoomCount())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalNights((int) nights)
                .totalPrice(totalPrice)
                .pricePerNight(roomType.getPricePerNight())
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .specialRequests(request.getSpecialRequests())
                .build();

        // Reduce room availability atomically
        roomTypeService.reduceAvailability(request.getRoomTypeId(), request.getRoomCount());

        return hotelBookingRepository.save(booking);
    }

    @Override
    @Transactional
    public HotelBooking updateStatus(Long bookingId, BookingStatus status) {
        HotelBooking booking = hotelBookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return null;

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);

        // If cancelling, restore room availability
        if (oldStatus == BookingStatus.CONFIRMED && status == BookingStatus.CANCELLED) {
            roomTypeService.restoreAvailability(booking.getRoomType().getId(), booking.getRoomCount());
        }

        return hotelBookingRepository.save(booking);
    }

    @Override
    public List<HotelBooking> getByUser(Long userId) {
        return hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<HotelBooking> getByProvider(Long providerId) {
        return hotelBookingRepository.findByHotelProviderIdOrderByCreatedAtDesc(providerId);
    }

    @Override
    public List<HotelBooking> getByHotel(Long hotelId) {
        return hotelBookingRepository.findByHotelId(hotelId);
    }

    @Override
    public List<HotelBooking> getAll() {
        return hotelBookingRepository.findAll();
    }

    @Override
    public HotelBooking getById(Long id) {
        return hotelBookingRepository.findById(id).orElse(null);
    }

    @Override
    public double getTotalRevenueForProvider(Long providerId) {
        return hotelBookingRepository.findByHotelProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(HotelBooking::getTotalPrice)
                .sum();
    }

    @Override
    public int getBookingCountForProvider(Long providerId) {
        return hotelBookingRepository.findByHotelProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .mapToInt(HotelBooking::getRoomCount)
                .sum();
    }

    public HotelBookingDTO toDTO(HotelBooking booking) {
        List<String> imageUrls = hotelImageService.getImageUrls(booking.getHotel().getId());
        return HotelBookingDTO.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getName())
                .hotelId(booking.getHotel().getId())
                .hotelName(booking.getHotel().getName())
                .hotelLocation(booking.getHotel().getLocation())
                .hotelImageUrl(imageUrls.isEmpty() ? booking.getHotel().getImageUrl() : imageUrls.get(0))
                .roomTypeId(booking.getRoomType().getId())
                .roomTypeName(booking.getRoomType().getName())
                .roomCount(booking.getRoomCount())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .totalNights(booking.getTotalNights())
                .totalPrice(booking.getTotalPrice())
                .pricePerNight(booking.getPricePerNight())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .specialRequests(booking.getSpecialRequests())
                .build();
    }
}
