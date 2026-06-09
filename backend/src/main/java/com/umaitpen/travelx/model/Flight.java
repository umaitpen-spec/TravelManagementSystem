package com.umaitpen.travelx.model;

import com.umaitpen.travelx.enums.FlightStatus;
import com.umaitpen.travelx.enums.FlightType;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String airline;

    private String flightNumber;
    private String origin;
    private String destination;
    private String sourceCity;
    private String destinationCity;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String duration;

    @Enumerated(EnumType.STRING)
    private FlightType flightType;

    private String baggageDetails;
    private String description;
    private String imageUrl;
    private String airlineLogo;
    private String amenities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FlightStatus status = FlightStatus.ACTIVE;

    private String cancellationPolicy;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlightClass> flightClasses = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = FlightStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}