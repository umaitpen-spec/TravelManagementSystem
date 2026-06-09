package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.dto.CreateFlightBookingRequest;
import com.umaitpen.travelx.enums.BookingStatus;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.FlightBooking;
import com.umaitpen.travelx.model.FlightClass;
import com.umaitpen.travelx.model.PassengerDetails;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.FlightBookingRepository;
import com.umaitpen.travelx.repository.FlightClassRepository;
import com.umaitpen.travelx.repository.FlightRepository;
import com.umaitpen.travelx.repository.PassengerDetailsRepository;
import com.umaitpen.travelx.repository.UserRepository;
import com.umaitpen.travelx.service.FlightBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightBookingServiceImpl implements FlightBookingService {
    private final FlightBookingRepository flightBookingRepository;
    private final FlightRepository flightRepository;
    private final FlightClassRepository flightClassRepository;
    private final PassengerDetailsRepository passengerDetailsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FlightBooking createBooking(Long userId, CreateFlightBookingRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId()).orElse(null);
        if (flight == null) {
            throw new IllegalArgumentException("Flight not found");
        }

        FlightClass flightClass = flightClassRepository.findById(request.getFlightClassId()).orElse(null);
        if (flightClass == null) {
            throw new IllegalArgumentException("Flight class not found");
        }

        if (flightClass.getAvailableSeats() < request.getPassengerCount()) {
            throw new IllegalStateException("Not enough seats available. Only " + flightClass.getAvailableSeats() + " seats left.");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        double pricePerTicket = flightClass.getPrice();
        double totalPrice = pricePerTicket * request.getPassengerCount();

        FlightBooking booking = FlightBooking.builder()
                .bookingReference("FB" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(BookingStatus.CONFIRMED)
                .user(user)
                .flight(flight)
                .flightClass(flightClass)
                .passengerCount(request.getPassengerCount())
                .totalPrice(totalPrice)
                .pricePerTicket(pricePerTicket)
                .travelDate(request.getTravelDate() != null ? request.getTravelDate() : flight.getDepartureDate())
                .bookingType("FLIGHT")
                .build();

        booking = flightBookingRepository.save(booking);

        if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
            for (CreateFlightBookingRequest.PassengerInfo pi : request.getPassengers()) {
                PassengerDetails passenger = PassengerDetails.builder()
                        .flightBooking(booking)
                        .passengerName(pi.getPassengerName())
                        .email(pi.getEmail())
                        .phone(pi.getPhone())
                        .gender(pi.getGender())
                        .age(pi.getAge())
                        .passengerType(pi.getPassengerType())
                        .build();
                passengerDetailsRepository.save(passenger);
            }
        }

        flightClass.setAvailableSeats(flightClass.getAvailableSeats() - request.getPassengerCount());
        flightClassRepository.save(flightClass);

        return booking;
    }

    @Override
    @Transactional
    public FlightBooking updateStatus(Long bookingId, BookingStatus status) {
        FlightBooking booking = flightBookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return null;

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);

        if (oldStatus == BookingStatus.CONFIRMED && status == BookingStatus.CANCELLED) {
            FlightClass fc = booking.getFlightClass();
            fc.setAvailableSeats(fc.getAvailableSeats() + booking.getPassengerCount());
            flightClassRepository.save(fc);
        }

        return flightBookingRepository.save(booking);
    }

    @Override
    public List<FlightBooking> getByUser(Long userId) {
        return flightBookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<FlightBooking> getByProvider(Long providerId) {
        return flightBookingRepository.findByFlightProviderIdOrderByCreatedAtDesc(providerId);
    }

    @Override
    public List<FlightBooking> getByFlight(Long flightId) {
        return flightBookingRepository.findByFlightId(flightId);
    }

    @Override
    public List<FlightBooking> getAll() {
        return flightBookingRepository.findAll();
    }

    @Override
    public FlightBooking getById(Long id) {
        return flightBookingRepository.findById(id).orElse(null);
    }

    @Override
    public double getTotalRevenueForProvider(Long providerId) {
        return flightBookingRepository.findByFlightProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(FlightBooking::getTotalPrice)
                .sum();
    }

    @Override
    public int getBookingCountForProvider(Long providerId) {
        return (int) flightBookingRepository.findByFlightProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }

    @Override
    public int getSeatsFilledForProvider(Long providerId) {
        return flightBookingRepository.findByFlightProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .mapToInt(FlightBooking::getPassengerCount)
                .sum();
    }
}
