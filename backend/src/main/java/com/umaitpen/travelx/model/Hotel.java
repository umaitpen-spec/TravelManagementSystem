package com.umaitpen.travelx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"roomTypes", "images", "provider"})
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;
    private String address;
    private String description;
    private String amenities;
    private Double rating;
    private String imageUrl;
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Double pricePerNight = 0.0;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomType> roomTypes = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HotelImage> images = new java.util.ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public int getTotalAvailableRooms() {
        return roomTypes.stream().mapToInt(RoomType::getAvailableCount).sum();
    }
}
