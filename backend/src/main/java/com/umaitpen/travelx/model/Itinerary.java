package com.umaitpen.travelx.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dayLabel; // e.g., Day 1
    @Column(columnDefinition = "TEXT")
    private String activitiesJson; // stored as JSON string for flexibility
    private Double estimatedCost;

    @Column(columnDefinition = "TEXT")
    private String planDetailsJson; // stores complete AI plan response (summary, budgetBreakdown, travelTips, etc.)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
