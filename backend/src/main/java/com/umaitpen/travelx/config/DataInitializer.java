package com.umaitpen.travelx.config;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.enums.FlightType;
import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.enums.SeatClass;
import com.umaitpen.travelx.model.Flight;
import com.umaitpen.travelx.model.FlightClass;
import com.umaitpen.travelx.model.Hotel;
import com.umaitpen.travelx.model.RoomType;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.FlightRepository;
import com.umaitpen.travelx.repository.HotelRepository;
import com.umaitpen.travelx.repository.RoomTypeRepository;
import com.umaitpen.travelx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        createAdminIfNotExists();
        createDemoUserIfNotExists();
        createDemoHotelProviderIfNotExists();
        createDemoFlightProviderIfNotExists();
        createSampleFlightIfNotExists();
        createSampleHotelIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (userRepository.findByEmail("admin@zoho.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@zoho.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin user created: admin@zoho.com / admin123");
        }
    }

    private void createDemoUserIfNotExists() {
        if (userRepository.findByEmail("test@zoho.com").isEmpty()) {
            User user = User.builder()
                    .name("Demo User")
                    .email("test@zoho.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Demo User created: test@zoho.com / 123456");
        }
    }

    private void createDemoHotelProviderIfNotExists() {
        if (userRepository.findByEmail("hotelprovider@demo.com").isEmpty()) {
            User provider = User.builder()
                    .name("Demo Hotel Provider")
                    .email("hotelprovider@demo.com")
                    .password(passwordEncoder.encode("provider123"))
                    .role(Role.ROLE_SERVICE_PROVIDER)
                    .providerType(ProviderType.HOTEL_PROVIDER)
                    .providerCompany("Demo Hotels")
                    .enabled(true)
                    .build();
            userRepository.save(provider);
            log.info("Demo Hotel Provider created: hotelprovider@demo.com / provider123");
        }
    }

    private void createDemoFlightProviderIfNotExists() {
        if (userRepository.findByEmail("flightprovider@demo.com").isEmpty()) {
            User provider = User.builder()
                    .name("Demo Flight Provider")
                    .email("flightprovider@demo.com")
                    .password(passwordEncoder.encode("provider123"))
                    .role(Role.ROLE_SERVICE_PROVIDER)
                    .providerType(ProviderType.FLIGHT_PROVIDER)
                    .providerCompany("Demo Airlines")
                    .enabled(true)
                    .build();
            userRepository.save(provider);
            log.info("Demo Flight Provider created: flightprovider@demo.com / provider123");
        }
    }

    private void createSampleFlightIfNotExists() {
        User flightProvider = userRepository.findByEmail("flightprovider@demo.com").orElse(null);
        if (flightProvider == null) return;

        if (flightRepository.count() > 0) {
            // Update existing flights with missing logo
            List<Flight> flights = flightRepository.findAll();
            for (Flight f : flights) {
                if (f.getAirlineLogo() == null || f.getAirlineLogo().isEmpty()) {
                    switch (f.getAirline()) {
                        case "Emirates" -> f.setAirlineLogo("/images/airlines/emirates.png");
                        case "Qatar Airways" -> f.setAirlineLogo("/images/airlines/qatar.png");
                        case "Singapore Airlines" -> f.setAirlineLogo("/images/airlines/singapore.png");
                        case "Air India" -> f.setAirlineLogo("/images/airlines/airindia.png");
                        case "Lufthansa" -> f.setAirlineLogo("/images/airlines/lufthansa.png");
                    }
                    flightRepository.save(f);
                }
            }
            return;
        }

        // Flight 1: Emirates
        Flight flight1 = createFlight("Emirates", "EK 567", "Delhi", "", "Delhi", "Mumbai",
                LocalDate.now().plusDays(7), LocalTime.of(9, 0), LocalTime.of(11, 30), "2h 30m",
                "/images/airlines/emirates.png",
                FlightType.DOMESTIC, flightProvider);
        flightRepository.save(flight1);

        // Flight 2: Qatar Airways
        Flight flight2 = createFlight("Qatar Airways", "QR 842", "Mumbai", "Bangalore", "Mumbai", "Bangalore",
                LocalDate.now().plusDays(5), LocalTime.of(14, 30), LocalTime.of(16, 45), "2h 15m",
                "/images/airlines/qatar.png",
                FlightType.DOMESTIC, flightProvider);
        flightRepository.save(flight2);

        // Flight 3: Singapore Airlines
        Flight flight3 = createFlight("Singapore Airlines", "SQ 421", "Delhi", "Singapore", "Delhi", "Singapore",
                LocalDate.now().plusDays(10), LocalTime.of(22, 0), LocalTime.of(6, 30), "5h 30m",
                "/images/airlines/singapore.png",
                FlightType.INTERNATIONAL, flightProvider);
        flightRepository.save(flight3);

        // Flight 4: Air India
        Flight flight4 = createFlight("Air India", "AI 305", "Kolkata", "Mumbai", "Kolkata", "Mumbai",
                LocalDate.now().plusDays(3), LocalTime.of(7, 15), LocalTime.of(10, 0), "2h 45m",
                "/images/airlines/airindia.png",
                FlightType.DOMESTIC, flightProvider);
        flightRepository.save(flight4);

        // Flight 5: Lufthansa
        Flight flight5 = createFlight("Lufthansa", "LH 762", "Mumbai", "Frankfurt", "Mumbai", "Frankfurt",
                LocalDate.now().plusDays(14), LocalTime.of(1, 30), LocalTime.of(7, 45), "9h 15m",
                "/images/airlines/lufthansa.png",
                FlightType.INTERNATIONAL, flightProvider);
        flightRepository.save(flight5);

        log.info("Sample flights created: Emirates, Qatar Airways, Singapore Airlines, Air India, Lufthansa");
    }

    private Flight createFlight(String airline, String flightNumber, String origin, String destination,
                               String sourceCity, String destCity, LocalDate depDate, LocalTime depTime,
                               LocalTime arrTime, String duration, String logo, FlightType type, User provider) {
        List<FlightClass> classes = new ArrayList<>();
        classes.add(FlightClass.builder()
                .classType(SeatClass.ECONOMY)
                .totalSeats(150)
                .availableSeats(150)
                .price(4500.0)
                .baggageLimit("23kg")
                .refundable(true)
                .build());
        classes.add(FlightClass.builder()
                .classType(SeatClass.PREMIUM_ECONOMY)
                .totalSeats(40)
                .availableSeats(40)
                .price(7500.0)
                .baggageLimit("32kg")
                .refundable(true)
                .build());
        classes.add(FlightClass.builder()
                .classType(SeatClass.BUSINESS)
                .totalSeats(20)
                .availableSeats(20)
                .price(15000.0)
                .baggageLimit("45kg")
                .refundable(true)
                .build());

        Flight flight = Flight.builder()
                .airline(airline)
                .flightNumber(flightNumber)
                .origin(origin)
                .destination(destination)
                .sourceCity(sourceCity)
                .destinationCity(destCity)
                .departureDate(depDate)
                .departureTime(depTime)
                .arrivalTime(arrTime)
                .duration(duration)
                .flightType(type)
                .baggageDetails("23kg check-in + 7kg cabin")
                .airlineLogo(logo)
                .status(FlightStatus.ACTIVE)
                .provider(provider)
                .flightClasses(classes)
                .build();

        classes.forEach(c -> c.setFlight(flight));
        return flight;
    }

    private void createSampleHotelIfNotExists() {
        User hotelProvider = userRepository.findByEmail("hotelprovider@demo.com").orElse(null);
        if (hotelProvider == null) return;

        if (hotelRepository.count() > 0) {
            return;
        }

        // Hotel 1: The Grand Palace Hotel - Mumbai
        List<RoomType> roomTypes1 = createRoomTypes();
        Hotel hotel1 = Hotel.builder()
                .name("The Grand Palace Hotel")
                .location("Mumbai")
                .address("Marine Drive, Mumbai 400001")
                .description("Luxury 5-star hotel with stunning views of the Arabian Sea. Experience world-class hospitality with premium amenities and exceptional service.")
                .amenities("Swimming Pool, Spa, Gym, Free WiFi, Restaurant, Bar, Room Service, Concierge")
                .rating(4.8)
                .pricePerNight(2500.0)
                .imageUrl("https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800")
                .active(true)
                .provider(hotelProvider)
                .roomTypes(roomTypes1)
                .build();
        roomTypes1.forEach(r -> r.setHotel(hotel1));
        hotelRepository.save(hotel1);

        // Hotel 2: Taj Palace Hotel - Delhi
        List<RoomType> roomTypes2 = createRoomTypes();
        Hotel hotel2 = Hotel.builder()
                .name("Taj Palace Hotel")
                .location("Delhi")
                .address("Sardar Patel Marg, Delhi 110021")
                .description("Historic luxury hotel offering world-renowned hospitality with modern amenities. Located in the heart of Delhi with easy access to business and cultural districts.")
                .amenities("Swimming Pool, Spa, Gym, Free WiFi, 3 Restaurants, Bar, Business Center, Airport Shuttle")
                .rating(4.6)
                .pricePerNight(3500.0)
                .imageUrl("https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800")
                .active(true)
                .provider(hotelProvider)
                .roomTypes(roomTypes2)
                .build();
        roomTypes2.forEach(r -> r.setHotel(hotel2));
        hotelRepository.save(hotel2);

        // Hotel 3: Sea View Resort - Goa
        List<RoomType> roomTypes3 = createRoomTypes();
        Hotel hotel3 = Hotel.builder()
                .name("Sea View Resort")
                .location("Goa")
                .address("Baga Beach, Goa 403516")
                .description("Beachfront paradise with direct beach access. Perfect for families and couples seeking sun, sand, and relaxation with Goa's vibrant culture.")
                .amenities("Private Beach, Swimming Pool, Free WiFi, Beach Bar, Restaurant, Water Sports, Kids Club")
                .rating(4.5)
                .pricePerNight(1800.0)
                .imageUrl("https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800")
                .active(true)
                .provider(hotelProvider)
                .roomTypes(roomTypes3)
                .build();
        roomTypes3.forEach(r -> r.setHotel(hotel3));
        hotelRepository.save(hotel3);

        // Hotel 4: The Royal Park - Bangalore
        List<RoomType> roomTypes4 = createRoomTypes();
        Hotel hotel4 = Hotel.builder()
                .name("The Royal Park")
                .location("Bangalore")
                .address("MG Road, Bangalore 560001")
                .description("Boutique luxury hotel in the heart of Bangalore's shopping and entertainment district. Known for exceptional service and boutique charm.")
                .amenities("Rooftop Restaurant, Spa, Free WiFi, Gym, Bar, Concierge, City Views")
                .rating(4.7)
                .pricePerNight(2800.0)
                .imageUrl("https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=800")
                .active(true)
                .provider(hotelProvider)
                .roomTypes(roomTypes4)
                .build();
        roomTypes4.forEach(r -> r.setHotel(hotel4));
        hotelRepository.save(hotel4);

        // Hotel 5: Mountain View Inn - Manali
        List<RoomType> roomTypes5 = createRoomTypes();
        Hotel hotel5 = Hotel.builder()
                .name("Mountain View Inn")
                .location("Manali")
                .address("Hadimba Temple Road, Manali 175131")
                .description("Cozy mountain retreat with panoramic views of the Himalayas. Perfect for adventure seekers and nature lovers seeking tranquility.")
                .amenities("Heated Rooms, Free WiFi, Restaurant, Bonfire, Trekking Tours, Airport Shuttle, Snow View")
                .rating(4.4)
                .pricePerNight(1500.0)
                .imageUrl("https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800")
                .active(true)
                .provider(hotelProvider)
                .roomTypes(roomTypes5)
                .build();
        roomTypes5.forEach(r -> r.setHotel(hotel5));
        hotelRepository.save(hotel5);

        log.info("Sample hotels created: The Grand Palace Hotel, Taj Palace Hotel, Sea View Resort, The Royal Park, Mountain View Inn");
    }

    private List<RoomType> createRoomTypes() {
        List<RoomType> roomTypes = new ArrayList<>();
        roomTypes.add(RoomType.builder()
                .name("Standard Room")
                .pricePerNight(2500.0)
                .availableCount(10)
                .capacity(2)
                .ac(true)
                .features("King Bed, Free WiFi, Breakfast Included, City View")
                .build());
        roomTypes.add(RoomType.builder()
                .name("Deluxe Room")
                .pricePerNight(4500.0)
                .availableCount(8)
                .capacity(2)
                .ac(true)
                .features("King Bed, Free WiFi, Breakfast Included, City View, Minibar, Balcony")
                .build());
        roomTypes.add(RoomType.builder()
                .name("Suite")
                .pricePerNight(8000.0)
                .availableCount(4)
                .capacity(4)
                .ac(true)
                .features("King Bed, Free WiFi, Breakfast Included, City View, Minibar, Balcony, Living Area, Jacuzzi")
                .build());
        return roomTypes;
    }
}
