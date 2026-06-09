package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses WHERE f.id = :id")
    Flight findByIdWithClasses(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses WHERE f.provider.id = :providerId")
    List<Flight> findByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses ORDER BY f.createdAt DESC")
    List<Flight> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses WHERE f.status = :status")
    List<Flight> findByStatus(@Param("status") FlightStatus status);

    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses WHERE f.provider.id = :providerId ORDER BY f.createdAt DESC")
    List<Flight> findByProviderIdOrderByCreatedAtDesc(@Param("providerId") Long providerId);

    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.flightClasses WHERE f.status = 'ACTIVE' " +
           "AND (:source IS NULL OR LOWER(f.sourceCity) LIKE LOWER(CONCAT('%', :source, '%'))) " +
           "AND (:destination IS NULL OR LOWER(f.destinationCity) LIKE LOWER(CONCAT('%', :destination, '%'))) " +
           "AND (:departureDate IS NULL OR f.departureDate >= :departureDate)")
    List<Flight> searchFlights(@Param("source") String source,
                               @Param("destination") String destination,
                               @Param("departureDate") LocalDate departureDate);


    @Query("SELECT f FROM Flight f WHERE f.status = 'ACTIVE' " +
           "AND LOWER(f.sourceCity) LIKE LOWER(CONCAT('%', :source, '%')) " +
           "AND LOWER(f.destinationCity) LIKE LOWER(CONCAT('%', :destination, '%'))")
    List<Flight> findByOriginAndDestination(@Param("source") String source, @Param("destination") String destination);
}

