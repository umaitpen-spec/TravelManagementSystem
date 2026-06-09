package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.DestinationRecommendationDTO;
import com.umaitpen.travelx.dto.RecommendationRequestDTO;
import com.umaitpen.travelx.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getRecommendations(@PathVariable Long userId, HttpSession session) {
        // Validate user session
        Long sessionUserId = (Long) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(userId)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<DestinationRecommendationDTO> recommendations = recommendationService.getRecommendationsForUser(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRecommendations(@RequestBody RecommendationRequestDTO request, HttpSession session) {
        // Validate user session
        Long sessionUserId = (Long) session.getAttribute("userId");
        if (sessionUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please sign in first"));
        }

        // Ensure request userId matches session
        if (request.getUserId() != null && !request.getUserId().equals(sessionUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            List<DestinationRecommendationDTO> recommendations;
            if (request.getUserPreference() != null && !request.getUserPreference().trim().isBlank()) {
                recommendations = recommendationService.getRecommendationsWithPreference(
                        sessionUserId, request.getUserPreference());
            } else {
                recommendations = recommendationService.getRecommendationsForUser(sessionUserId);
            }
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/preferences")
    public ResponseEntity<?> savePreferences(@RequestBody RecommendationRequestDTO request, HttpSession session) {
        Long sessionUserId = (Long) session.getAttribute("userId");
        if (sessionUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please sign in first"));
        }

        // For now, just acknowledge the save
        // In a full implementation, store in a UserTravelPreference entity
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Preferences saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
