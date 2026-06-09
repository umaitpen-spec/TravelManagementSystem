package com.umaitpen.travelx.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umaitpen.travelx.dto.DestinationRecommendationDTO;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.max-plans-per-trip:3}")
    private int maxPlansPerTrip;

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.site.url:http://localhost:8080}")
    private String siteUrl;

    @Value("${openrouter.app.name:TravelX}")
    private String appName;

    @Value("${openrouter.http.connect-timeout-ms:10000}")
    private long connectTimeoutMs;

    @Value("${openrouter.http.read-timeout-ms:60000}")
    private long readTimeoutMs;

    public AIServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs))
                .build();
    }

    public int getMaxPlansPerTrip() {
        return maxPlansPerTrip;
    }

    @Override
    public String generateItineraryJson(Trip trip) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Using fallback itinerary because OPENROUTER_API_KEY is not configured.");
            return fallbackItinerary(trip, 1);
        }

        try {
            log.info("Generating AI itinerary with OpenRouter model '{}' for destination '{}'.", model, trip.getDestination());
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("stream", false);
            request.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", "You are a travel planning assistant. Return only valid JSON with no markdown."
                    ),
                    Map.of(
                            "role", "user",
                            "content", buildPrompt(trip)
                    )
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", siteUrl);
            headers.set("X-Title", appName);

            String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(request, headers), String.class);
            log.info("OpenRouter raw response preview: {}", response == null ? "null" : response.substring(0, Math.min(300, response.length())));

            String content = extractAssistantContent(response);
            log.info("OpenRouter extracted content preview: {}", content == null ? "null" : content.substring(0, Math.min(300, content.length())));
            return normalizeJson(content, trip, 1);


        } catch (HttpStatusCodeException e) {
            log.warn("Using fallback itinerary because OpenRouter returned HTTP {}: {}",
                    e.getRawStatusCode(),
                    e.getResponseBodyAsString());
            return fallbackItinerary(trip, 1);
        } catch (Exception e) {
            log.warn("Using fallback itinerary because AI itinerary generation failed: {}", e.getMessage());
            return fallbackItinerary(trip, 1);
        }
    }

    @Override
    public List<String> generateMultipleItineraryPlans(Trip trip, String preferences) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Using fallback itinerary because OPENROUTER_API_KEY is not configured.");
            return List.of(fallbackItinerary(trip, 1));
        }

        int plansToGenerate = Math.min(maxPlansPerTrip, 3);
        final String finalPreferences = preferences;
        return java.util.stream.IntStream.range(0, plansToGenerate)
                .mapToObj(i -> {
                    try {
                        log.info("Generating AI itinerary plan {} of {} with OpenRouter model '{}' for destination '{}'.",
                                i + 1, plansToGenerate, model, trip.getDestination());
                        Map<String, Object> request = new HashMap<>();
                        request.put("model", model);
                        request.put("stream", false);
                        request.put("messages", List.of(
                                Map.of(
                                        "role", "system",
                                        "content", "You are a travel planning assistant. Return only valid JSON with no markdown."
                                ),
                                Map.of(
                                        "role", "user",
                                        "content", buildPrompt(trip, i, finalPreferences)
                                )
                        ));

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(apiKey);
                        headers.set("HTTP-Referer", siteUrl);
                        headers.set("X-Title", appName);

                        String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(request, headers), String.class);
                        String content = extractAssistantContent(response);
                        return normalizeJson(content, trip, 1);
                    } catch (HttpStatusCodeException e) {
                        log.warn("Using fallback for plan {} because OpenRouter returned HTTP {}: {}",
                                i + 1, e.getRawStatusCode(), e.getResponseBodyAsString());
                        return fallbackItinerary(trip, i + 1);
                    } catch (Exception e) {
                        log.warn("Using fallback for plan {} because AI generation failed: {}", i + 1, e.getMessage());
                        return fallbackItinerary(trip, i + 1);
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String generateSingleItineraryPlan(Trip trip, int planNumber, String preferences) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Using fallback itinerary because OPENROUTER_API_KEY is not configured.");
            return fallbackItinerary(trip, planNumber);
        }

        int index = planNumber - 1;
        if (index < 0 || index >= 3) {
            index = 0;
        }

        try {
            log.info("Generating AI itinerary plan {} of 3 with OpenRouter model '{}' for destination '{}'.",
                    planNumber, model, trip.getDestination());
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("stream", false);
            request.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", "You are a travel planning assistant. Return only valid JSON with no markdown."
                    ),
                    Map.of(
                            "role", "user",
                            "content", buildPrompt(trip, index, preferences)
                    )
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", siteUrl);
            headers.set("X-Title", appName);

            String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(request, headers), String.class);
            String content = extractAssistantContent(response);
            return normalizeJson(content, trip, planNumber);
        } catch (HttpStatusCodeException e) {
            log.warn("Using fallback for plan {} because OpenRouter returned HTTP {}: {}",
                    planNumber, e.getRawStatusCode(), e.getResponseBodyAsString());
            return fallbackItinerary(trip, planNumber);
        } catch (Exception e) {
            log.warn("Using fallback for plan {} because AI generation failed: {}", planNumber, e.getMessage());
            return fallbackItinerary(trip, planNumber);
        }
    }

   private String buildPrompt(Trip trip, int variation, String preferences) {
        long durationDays = calculateDurationDays(trip);

        String[] variationFocus = {
            "Focus on popular tourist attractions, iconic landmarks, and must-see experiences. Include famous restaurants and cultural hotspots.",
            "Focus on off-the-beaten-path experiences, hidden gems, local neighborhoods, authentic food spots, and unique cultural encounters.",
            "Focus on a balanced mix of relaxation and adventure. Include nature activities, scenic spots, wellness experiences, and moderate adventure."
        };

        String focus = (variation >= 0 && variation < 3) ? variationFocus[variation] : variationFocus[0];

        String preferencesSection = "";
        if (preferences != null && !preferences.trim().isBlank()) {
            preferencesSection = "User Preferences: " + preferences.trim() + ". "
                    + "You MUST incorporate these preferences into the itinerary where possible. "
                    + "If the user mentions specific places, activities, or themes (e.g., 'temples', 'theme parks', 'shopping', 'street food'), "
                    + "ensure they are included in the daily plan. ";
        }

        // Weather conditions for travel planning
        String weatherGuidance = getWeatherGuidanceForDestination(trip.getDestination());

        return "You are an intelligent AI Travel Planner for TravelX, inspired by Google Travel AI, TripAdvisor smart planner, and Expedia AI itinerary systems. "

                + "IMPORTANT: Return ONLY valid JSON. "
                + "Do NOT include markdown. "
                + "Do NOT include explanations. "
                + "Do NOT include ```json blocks. "
                + "Do NOT include comments. "
                + "Do NOT include any text before or after JSON. "
                + "The response must be directly parsable by Jackson ObjectMapper. "

                + "ALL JSON fields must ALWAYS be present. Never omit fields even if data is unavailable. Use \"Unknown\" or empty arrays [] or 0 as fallback. "

                + "Generate a highly personalized, practical, budget-aware, and engaging travel itinerary with INTELLIGENT WEATHER-AWARE PLANNING and REALISTIC TRAVEL LOGISTICS. "

                + preferencesSection

                + "Variation " + (variation + 1) + " of 3 - " + focus + " "

                + "=== WEATHER-AWARE PLANNING ===\n"
                + "Expected weather conditions for this destination: " + weatherGuidance + "\n"
                + "- For SUNNY/HOT days: Schedule outdoor activities for early morning or late afternoon. Suggest indoor alternatives during peak heat hours (12 PM - 4 PM).\n"
                + "- For RAINY days: Prioritize indoor attractions (museums, temples, shopping malls, restaurants). Suggest indoor alternatives for outdoor activities.\n"
                + "- For PLEASANT days: Maximize outdoor sightseeing and nature activities.\n"
                + "- For COLD days: Suggest warm clothing layers, hot beverage stops, and indoor breaks.\n"
                + "- Always provide weather-based travel advice for each activity.\n"

                + "=== INDOOR/OUTDOOR BALANCING ===\n"
                + "- Balance each day's activities between indoor and outdoor to provide variety.\n"
                + "- During extreme weather (heavy rain, extreme heat), prefer fully indoor days.\n"
                + "- Suggest break points at cafes or restaurants during hot afternoons.\n"

                + "=== CROWD INTELLIGENCE ===\n"
                + "- For each attraction, provide crowd level: LOW, MODERATE, or HIGH.\n"
                + "- Include best visit timing to avoid peak crowds (e.g., 'Early morning', 'Weekday afternoons').\n"
                + "- Suggest crowd avoidance strategies (e.g., 'Avoid weekends', 'Book tickets online').\n"

                + "=== PRACTICAL TRAVEL INTELLIGENCE ===\n"
                + "- Museum/attraction closure days: Check typical closed days and include warnings if relevant.\n"
                + "- Public holiday warnings: Note if trip dates overlap with public holidays that may cause closures or crowds.\n"
                + "- Festival crowd alerts: Warn about local festivals that may cause increased crowds.\n"
                + "- Peak-hour traffic: Suggest best travel times to avoid traffic congestion.\n"
                + "- Realistic transport feasibility: Group nearby attractions. Don't schedule activities requiring 3+ hours of travel in a single day.\n"
                + "- Travel fatigue reduction: Don't stack more than 4 activities per day. Include rest breaks.\n"

                + "=== ATTRACTION BEST PRACTICES ===\n"
                + "- Mention if an attraction has specific closure days or maintenance schedules.\n"
                + "- Include ticket booking tips where advance booking is recommended.\n"
                + "- Note if attractions have free entry days or discounted hours.\n"

                + "The itinerary must strictly fit within the given number of days and total budget. "

                + "Do NOT force exactly one activity per day. "
                + "Some days may contain multiple activities depending on travel feasibility and nearby attractions. "

                + "Create realistic travel plans including famous attractions, local food, hidden gems, cultural experiences, relaxation spots, shopping, and nightlife where appropriate. "

                + "Group nearby attractions together intelligently to minimize travel time and unnecessary expenses. "

                + "Avoid generic activities. "
                + "Provide meaningful activity descriptions and realistic recommendations. "

                + "Budget Rules: "
                + "Carefully distribute budget across accommodation, food, transport, and activities. "
                + "Avoid luxury suggestions for low budgets. "

                + "ALL estimatedCost values MUST be numbers only. "
                + "Do NOT use currency symbols. "
                + "Do NOT use commas in numbers. "

                + "=== ENHANCED JSON SCHEMA ===\n"
                + "Return JSON EXACTLY in this schema (all fields mandatory): "

                + "{"
                + "\"planId\":" + variation + ","
                + "\"planLabel\":\"Plan " + (variation + 1) + "\","
                + "\"destination\":\"string\","
                + "\"durationDays\":number,"
                + "\"travelers\":number,"
                + "\"budget\":number,"
                + "\"summary\":\"string\","
                + "\"overallWeather\":{\"condition\":\"string\",\"temperatureC\":number,\"travelAdvice\":\"string\"},"
                + "\"itinerary\":["
                + "{"
                + "\"day\":\"string\","
                + "\"theme\":\"string\","
                + "\"date\":\"string\","
                + "\"weather\":{\"condition\":\"string\",\"temperatureC\":number,\"travelAdvice\":\"string\"},"
                + "\"activities\":["
                + "{"
                + "\"time\":\"string\","
                + "\"title\":\"string\","
                + "\"description\":\"string\","
                + "\"location\":\"string\","
                + "\"estimatedCost\":number,"
                + "\"travelTip\":\"string\","
                + "\"weather\":{\"condition\":\"string\",\"temperatureC\":number,\"travelAdvice\":\"string\"},"
                + "\"crowdLevel\":\"string\","
                + "\"bestVisitTime\":\"string\","
                + "\"specialNotes\":[\"string\"]"
                + "}"
                + "],"
                + "\"dailyEstimatedBudget\":number,"
                + "\"dailyTotalCost\":number,"
                + "\"notes\":\"string\""
                + "}"
                + "],"
                + "\"budgetBreakdown\":{"
                + "\"accommodation\":number,"
                + "\"food\":number,"
                + "\"transport\":number,"
                + "\"activities\":number,"
                + "\"miscellaneous\":number"
                + "},"
                + "\"travelTips\":[\"string\"]"
                + "}. "

                + "Use INR internally but return only numeric values. "

                + "Weather condition values: Sunny, Cloudy, Rainy, Stormy, Snowy, Pleasant, Humid, Foggy, Windy\n"
                + "Crowd level values: Low, Moderate, High\n"
                + "Best visit time values: Early Morning (6-9 AM), Morning (9-12 PM), Afternoon (12-3 PM), Late Afternoon (3-6 PM), Evening (6-9 PM), Night (9 PM+), Weekdays Only, Weekends Only, Anytime\n"

                + "=== DYNAMIC DATA INJECTION (Future-Ready) ===\n"
                + "The prompt architecture supports later injection of:\n"
                + "- Live weather API data via {weatherApiData} placeholder\n"
                + "- Public holiday API data via {holidayApiData} placeholder\n"
                + "- Crowd/traffic metadata via {trafficApiData} placeholder\n"
                + "These can be added without major refactoring.\n"

                + "Trip details: "
                + "destination=" + trip.getDestination()
                + ", startDate=" + trip.getStartDate()
                + ", endDate=" + trip.getEndDate()
                + ", durationDays=" + durationDays
                + ", travelers=" + trip.getTravelers()
                + ", budgetINR=" + trip.getBudget() + ".";
    }

    /**
     * Returns weather guidance based on destination for realistic weather-aware planning.
     * This can later be replaced with actual weather API calls.
     */
    private String getWeatherGuidanceForDestination(String destination) {
        if (destination == null) return "Pleasant conditions expected. Pack layers and comfortable walking shoes.";
        String dest = destination.toLowerCase();
        if (dest.contains("manali") || dest.contains("shimla") || dest.contains("darjeeling") || dest.contains("leh") || dest.contains("ladakh") || dest.contains("gangtok") || dest.contains("nainital")) {
            return "Mountain hill station - expect cold mornings/evenings, pleasant daytime. Rain possible in monsoon (Jul-Sep). Pack layers, sunscreen.";
        } else if (dest.contains("goa") || dest.contains("kerala") || dest.contains("mumbai") || dest.contains("chennai") || dest.contains("pune")) {
            return "Coastal area - hot and humid, especially Mar-June. Monsoon (Jun-Sep) brings rain. Best visited Oct-Mar. Morning/evening activities recommended.";
        } else if (dest.contains("jaipur") || dest.contains("jodhpur") || dest.contains("udaipur") || dest.contains("delhi") || dest.contains("agra")) {
            return "Desert/r北方 plains - hot summers (Apr-Jun), cool winters (Nov-Feb). Summers can be extreme. Indoor activities recommended afternoon during summer.";
        } else if (dest.contains("bangalore") || dest.contains("mysore") || dest.contains("coorg")) {
            return "Pleasant year-round. Moderate climate, occasional rain. Light layers sufficient. Pleasant for outdoor activities throughout day.";
        } else if (dest.contains("kolkata") || dest.contains("bhubaneswar") || dest.contains("puri")) {
            return "Humid subtropical - hot summers, monsoon rains (Jun-Sep). Winter (Nov-Feb) is pleasant. Pack umbrella during monsoon.";
        } else {
            return "Pleasant conditions expected. Pack layers and comfortable walking shoes. Check local weather before travel.";
        }
    }

//    private String buildPrompt(Trip trip, int variation, String preferences) {
//         long durationDays = calculateDurationDays(trip);

//         String[] variationFocus = {
//             "Focus on popular tourist attractions, iconic landmarks, and must-see experiences. Include famous restaurants and cultural hotspots.",
//             "Focus on off-the-beaten-path experiences, hidden gems, local neighborhoods, authentic food spots, and unique cultural encounters.",
//             "Focus on a balanced mix of relaxation and adventure. Include nature activities, scenic spots, wellness experiences, and moderate adventure."
//         };

//         String focus = (variation >= 0 && variation < 3) ? variationFocus[variation] : variationFocus[0];

//         String preferencesSection = "";
//         if (preferences != null && !preferences.trim().isBlank()) {
//             preferencesSection = "User Preferences: " + preferences.trim() + ". "
//                     + "You MUST incorporate these preferences into the itinerary where possible. "
//                     + "If the user mentions specific places, activities, or themes (e.g., 'temples', 'theme parks', 'shopping', 'street food'), "
//                     + "ensure they are included in the daily plan. ";
//         }

//         return "You are an intelligent AI Travel Planner for TravelX. "

//                 + "IMPORTANT: Return ONLY valid JSON. "
//                 + "Do NOT include markdown. "
//                 + "Do NOT include explanations. "
//                 + "Do NOT include ```json blocks. "
//                 + "Do NOT include comments. "
//                 + "Do NOT include any text before or after JSON. "
//                 + "The response must be directly parsable by Jackson ObjectMapper. "

//                 + "Generate a highly personalized, practical, budget-aware, and engaging travel itinerary. "

//                 + preferencesSection

//                 + "Variation " + (variation + 1) + " of 3 - " + focus + " "

//                 + "The itinerary must strictly fit within the given number of days and total budget. "

//                 + "Do NOT force exactly one activity per day. "
//                 + "Some days may contain multiple activities depending on travel feasibility and nearby attractions. "

//                 + "Create realistic travel plans including famous attractions, local food, hidden gems, cultural experiences, relaxation spots, shopping, and nightlife where appropriate. "

//                 + "Group nearby attractions together intelligently to minimize travel time and unnecessary expenses. "

//                 + "Avoid generic activities. "
//                 + "Provide meaningful activity descriptions and realistic recommendations. "

//                 + "Budget Rules: "
//                 + "Carefully distribute budget across accommodation, food, transport, and activities. "
//                 + "Avoid luxury suggestions for low budgets. "

//                 + "ALL estimatedCost values MUST be numbers only. "
//                 + "Do NOT use currency symbols. "
//                 + "Do NOT use commas in numbers. "

//                 + "Return JSON EXACTLY in this schema: "

//                 + "{"
//                 + "\"planId\":" + variation + ","
//                 + "\"planLabel\":\"Plan " + (variation + 1) + "\","
//                 + "\"destination\":\"string\","
//                 + "\"durationDays\":number,"
//                 + "\"travelers\":number,"
//                 + "\"budget\":number,"
//                 + "\"summary\":\"string\","
//                 + "\"itinerary\":["
//                 + "{"
//                 + "\"day\":\"string\","
//                 + "\"theme\":\"string\","
//                 + "\"activities\":["
//                 + "{"
//                 + "\"time\":\"string\","
//                 + "\"title\":\"string\","
//                 + "\"description\":\"string\","
//                 + "\"location\":\"string\","
//                 + "\"estimatedCost\":number,"
//                 + "\"travelTip\":\"string\""
//                 + "}"
//                 + "],"
//                 + "\"dailyEstimatedBudget\":number,"
//                 + "\"notes\":\"string\""
//                 + "}"
//                 + "],"
//                 + "\"budgetBreakdown\":{"
//                 + "\"accommodation\":number,"
//                 + "\"food\":number,"
//                 + "\"transport\":number,"
//                 + "\"activities\":number,"
//                 + "\"miscellaneous\":number"
//                 + "},"
//                 + "\"travelTips\":[\"string\"]"
//                 + "}. "

//                 + "Use INR internally but return only numeric values. "

//                 + "Trip details: "
//                 + "destination=" + trip.getDestination()
//                 + ", startDate=" + trip.getStartDate()
//                 + ", endDate=" + trip.getEndDate()
//                 + ", durationDays=" + durationDays
//                 + ", travelers=" + trip.getTravelers()
//                 + ", budgetINR=" + trip.getBudget() + ".";
//     }

    private String buildPrompt(Trip trip) {
        return buildPrompt(trip, 0, null);
    }
    
    // private String buildPrompt(Trip trip) {
    //     long durationDays = calculateDurationDays(trip);
    //     return "Generate a practical travel itinerary for this trip. "
    //             + "Return JSON exactly in this schema: "
    //             + "{\"destination\":\"string\",\"durationDays\":number,\"travelers\":number,\"budget\":number,"
    //             + "\"itinerary\":[{\"day\":\"Day 1\",\"activities\":[\"activity\"],\"estimatedCost\":number}]}. "
    //             + "Use INR for estimatedCost, keep activities concise, and make one itinerary item per day. "
    //             + "Trip details: destination=" + trip.getDestination()
    //             + ", startDate=" + trip.getStartDate()
    //             + ", endDate=" + trip.getEndDate()
    //             + ", durationDays=" + durationDays
    //             + ", travelers=" + trip.getTravelers()
    //             + ", budgetINR=" + trip.getBudget() + ".";
    // }

    private String extractAssistantContent(String response) throws Exception {
        try {
            JsonNode root = mapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenRouter response did not include choices.");
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("OpenRouter response did not include message content.");
            }
            return content.asText();
        } catch (Exception parseEx) {
            // OpenRouter sometimes returns non-JSON text (e.g., safety messages). Log it and surface
            // a clearer error so we don't silently fail.
            String preview = response == null ? "null" : response.substring(0, Math.min(500, response.length()));
            throw new IllegalStateException("OpenRouter response was not JSON. Preview: " + preview, parseEx);
        }
    }


    private String normalizeJson(String content, Trip trip, int planNumber) throws Exception {
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }

        // OpenRouter sometimes returns extra prefixes/safety text before the JSON.
        // Strip everything before the first '{' so Jackson can parse reliably.
        int firstBrace = json.indexOf('{');
        if (firstBrace > 0) {
            json = json.substring(firstBrace).trim();
        }

        // Replace curly/smart quotes with straight ASCII quotes before parsing
        json = json.replaceAll("[\u2018\u2019]", "'").replaceAll("[\u201C\u201D]", "\"");

        Map<String, Object> parsed = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        // Force planId and planLabel to match the actual plan number
        int effectivePlanNum = (planNumber >= 1 && planNumber <= 3) ? planNumber : 1;
        parsed.put("planId", effectivePlanNum - 1);
        parsed.put("planLabel", "Plan " + effectivePlanNum);

        parsed.putIfAbsent("destination", trip.getDestination());
        parsed.putIfAbsent("durationDays", calculateDurationDays(trip));
        parsed.putIfAbsent("travelers", trip.getTravelers());
        parsed.putIfAbsent("budget", trip.getBudget());
        parsed.putIfAbsent("itinerary", Collections.emptyList());

        // Ensure overallWeather exists with defaults
        if (!parsed.containsKey("overallWeather") || parsed.get("overallWeather") == null) {
            parsed.put("overallWeather", Map.of(
                "condition", "Pleasant",
                "temperatureC", 25,
                "travelAdvice", "Check local weather forecast before travel."
            ));
        }

        // Normalize each day's activities to ensure new fields exist
        List<Map<String, Object>> itinerary = (List<Map<String, Object>>) parsed.get("itinerary");
        if (itinerary != null) {
            for (Map<String, Object> day : itinerary) {
                // Ensure day-level weather
                if (!day.containsKey("weather") || day.get("weather") == null) {
                    day.put("weather", Map.of(
                        "condition", "Pleasant",
                        "temperatureC", 25,
                        "travelAdvice", "Good weather for activities."
                    ));
                }
                // Ensure dailyTotalCost
                if (!day.containsKey("dailyTotalCost")) {
                    day.put("dailyTotalCost", day.get("dailyEstimatedBudget"));
                }
                // Normalize each activity
                List<Map<String, Object>> activities = (List<Map<String, Object>>) day.get("activities");
                if (activities != null) {
                    for (Map<String, Object> activity : activities) {
                        ensureActivityFields(activity);
                    }
                }
            }
        }

        return mapper.writeValueAsString(parsed);
    }

    private void ensureActivityFields(Map<String, Object> activity) {
        // Weather field
        if (!activity.containsKey("weather") || activity.get("weather") == null) {
            activity.put("weather", Map.of(
                "condition", "Pleasant",
                "temperatureC", 25,
                "travelAdvice", "Check weather before visiting."
            ));
        }
        // Crowd level
        if (!activity.containsKey("crowdLevel")) {
            activity.put("crowdLevel", "Moderate");
        }
        // Best visit time
        if (!activity.containsKey("bestVisitTime")) {
            activity.put("bestVisitTime", "Anytime");
        }
        // Special notes
        if (!activity.containsKey("specialNotes")) {
            activity.put("specialNotes", List.of());
        }
    }

    private long calculateDurationDays(Trip trip) {
        if (trip.getStartDate() == null || trip.getEndDate() == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1);
    }

    private String fallbackItinerary(Trip trip, int planNumber) {
        int effectiveNum = (planNumber >= 1 && planNumber <= 3) ? planNumber : 1;
        try {
            Map<String, Object> root = new HashMap<>();
            root.put("planId", effectiveNum - 1);
            root.put("planLabel", "Plan " + effectiveNum);
            root.put("destination", trip.getDestination());
            root.put("durationDays", calculateDurationDays(trip));
            root.put("travelers", trip.getTravelers());
            root.put("budget", trip.getBudget());
            root.put("summary", "A simple itinerary for your trip to " + trip.getDestination() + " (Plan " + effectiveNum + ")");

            // Overall weather
            Map<String, Object> overallWeather = new HashMap<>();
            overallWeather.put("condition", "Pleasant");
            overallWeather.put("temperatureC", 25);
            overallWeather.put("travelAdvice", "Pack light layers and comfortable shoes for daily exploration.");
            root.put("overallWeather", overallWeather);

            Map<String, Object> day1 = new HashMap<>();
            day1.put("day", "Day 1");
            day1.put("theme", "Arrival & Exploration");
            day1.put("date", trip.getStartDate() != null ? trip.getStartDate().toString() : "Day 1");

            // Day weather
            Map<String, Object> dayWeather = new HashMap<>();
            dayWeather.put("condition", "Pleasant");
            dayWeather.put("temperatureC", 25);
            dayWeather.put("travelAdvice", "Good weather for outdoor activities. Stay hydrated.");
            day1.put("weather", dayWeather);

            List<Map<String, Object>> activities = List.of(
                    Map.ofEntries(
                            Map.entry("time", "9:00 AM"),
                            Map.entry("title", "Arrival & Hotel check-in"),
                            Map.entry("description", "Check into your accommodation and settle in"),
                            Map.entry("location", "Airport/Station"),
                            Map.entry("estimatedCost", 1000),
                            Map.entry("travelTip", "Keep ID ready for check-in"),
                            Map.entry("weather", Map.of("condition", "Pleasant", "temperatureC", 25, "travelAdvice", "Indoor activity - AC check-in counter")),
                            Map.entry("crowdLevel", "Moderate"),
                            Map.entry("bestVisitTime", "Anytime"),
                            Map.entry("specialNotes", List.of("Confirm booking in advance", "Check-out time is usually 11 AM"))
                    ),
                    Map.ofEntries(
                            Map.entry("time", "12:00 PM"),
                            Map.entry("title", "Local market visit"),
                            Map.entry("description", "Explore local markets and shops"),
                            Map.entry("location", "City market"),
                            Map.entry("estimatedCost", 500),
                            Map.entry("travelTip", "Carry small denominations for purchases"),
                            Map.entry("weather", Map.of("condition", "Sunny", "temperatureC", 28, "travelAdvice", "Wear sunscreen and sunglasses")),
                            Map.entry("crowdLevel", "High"),
                            Map.entry("bestVisitTime", "Weekday mornings"),
                            Map.entry("specialNotes", List.of("Cash is king in local markets", "Bargaining is expected"))
                    ),
                    Map.ofEntries(
                            Map.entry("time", "7:00 PM"),
                            Map.entry("title", "Dinner at local restaurant"),
                            Map.entry("description", "Enjoy authentic local cuisine"),
                            Map.entry("location", "Local eatery"),
                            Map.entry("estimatedCost", 1500),
                            Map.entry("travelTip", "Ask for mildly spiced food if sensitive to spice"),
                            Map.entry("weather", Map.of("condition", "Pleasant", "temperatureC", 24, "travelAdvice", "Evening outdoor dining - pleasant weather")),
                            Map.entry("crowdLevel", "Moderate"),
                            Map.entry("bestVisitTime", "Evening (6-9 PM)"),
                            Map.entry("specialNotes", List.of("Most restaurants close kitchen by 10 PM"))
                    )
            );
            day1.put("activities", activities);
            day1.put("dailyEstimatedBudget", 3000);
            day1.put("dailyTotalCost", 3000);
            day1.put("notes", "Explore local culture on foot");

            root.put("itinerary", new Map[]{day1});

            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("accommodation", 0);
            breakdown.put("food", 2000);
            breakdown.put("transport", 500);
            breakdown.put("activities", 500);
            breakdown.put("miscellaneous", 0);
            root.put("budgetBreakdown", breakdown);

            root.put("travelTips", List.of("Bring comfortable walking shoes", "Keep local currency handy", "Stay hydrated", "Download offline maps", "Save emergency contacts locally"));
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public List<DestinationRecommendationDTO> getDestinationRecommendations(Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Using fallback recommendations because OPENROUTER_API_KEY is not configured.");
            return getFallbackRecommendations();
        }

        try {
            log.info("Generating AI destination recommendations with OpenRouter model '{}'.", model);
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("stream", false);
            request.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", "You are an intelligent AI Travel Recommendation assistant. Return ONLY valid JSON array."
                    ),
                    Map.of(
                            "role", "user",
                            "content", buildRecommendationPrompt(context)
                    )
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", siteUrl);
            headers.set("X-Title", appName);

            String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(request, headers), String.class);
            String content = extractAssistantContent(response);
            return parseRecommendations(content);

        } catch (HttpStatusCodeException e) {
            log.warn("Using fallback recommendations because OpenRouter returned HTTP {}: {}",
                    e.getRawStatusCode(), e.getResponseBodyAsString());
            return getFallbackRecommendations();
        } catch (Exception e) {
            log.warn("Using fallback recommendations because AI generation failed: {}", e.getMessage());
            return getFallbackRecommendations();
        }
    }

    private String buildRecommendationPrompt(Map<String, Object> context) {
        String userPreference = (String) context.getOrDefault("userPreference", "");
        String userHistory = (String) context.getOrDefault("userHistory", "No previous trips");
        String userBudget = (String) context.getOrDefault("userBudget", "moderate");
        String currentWeather = (String) context.getOrDefault("currentWeather", "Pleasant");
        String preferredClimate = (String) context.getOrDefault("preferredClimate", "");

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an intelligent AI Travel Recommendation assistant for Roamly Travel Platform.\n");
        prompt.append("IMPORTANT: Return ONLY valid JSON array. Do NOT include markdown. Do NOT include explanations.\n");
        prompt.append("Return a JSON array of 3 destination recommendations in this exact schema:\n");
        prompt.append("[{\"destination\":\"string\",\"description\":\"string\",\"currentWeather\":\"string\",\"weatherIcon\":\"string\",\"estimatedBudgetMin\":\"string\",\"estimatedBudgetMax\":\"string\",\"bestTimeToVisit\":\"string\",\"recommendationReason\":\"string\",\"suggestedActivities\":[\"string\"],\"tags\":[\"string\"],\"imageUrl\":\"string\"}]\n");
        prompt.append("Weather icons must be one of: bi-sun, bi-cloud-sun, bi-cloud, bi-rain, bi-snow, bi-lightning, bi-thermometer-sun\n");
        prompt.append("Tags must be from: Hill Station, Beach, Adventure, Family, Budget, Luxury, Nature, Culture, Romantic, Wildlife\n");
        prompt.append("Use placeholder image URLs like: https://images.unsplash.com/photo-1506905925346-21bda4d32df4 for mountains\n");
        prompt.append("User Preferences: ").append(userPreference).append("\n");
        prompt.append("User Travel History: ").append(userHistory).append("\n");
        prompt.append("User Budget Level: ").append(userBudget).append("\n");
        prompt.append("Current Weather Condition: ").append(currentWeather).append("\n");
        prompt.append("Preferred Climate: ").append(preferredClimate).append("\n");
        prompt.append("Generate 3 personalized recommendations that match the user's preferences, history, and current weather conditions.\n");
        prompt.append("Each recommendation should have estimated budget in INR (e.g., '₹8,000 - ₹12,000').\n");
        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private List<DestinationRecommendationDTO> parseRecommendations(String content) {
        try {
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
            int firstBrace = json.indexOf('[');
            if (firstBrace > 0) {
                json = json.substring(firstBrace).trim();
            }
            json = json.replaceAll("[\u2018\u2019]", "'").replaceAll("[\u201C\u201D]", "\"");

            List<Map<String, Object>> parsed = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            return parsed.stream().map(item -> {
                List<String> activities = item.get("suggestedActivities") != null
                    ? (List<String>) item.get("suggestedActivities")
                    : List.of("Sightseeing", "Local exploration");
                List<String> tags = item.get("tags") != null
                    ? (List<String>) item.get("tags")
                    : List.of("Travel");

                return DestinationRecommendationDTO.builder()
                        .destination((String) item.getOrDefault("destination", "Unknown"))
                        .description((String) item.getOrDefault("description", ""))
                        .currentWeather((String) item.getOrDefault("currentWeather", "Pleasant"))
                        .weatherIcon((String) item.getOrDefault("weatherIcon", "bi-sun"))
                        .estimatedBudgetMin((String) item.getOrDefault("estimatedBudgetMin", "₹5,000"))
                        .estimatedBudgetMax((String) item.getOrDefault("estimatedBudgetMax", "₹10,000"))
                        .bestTimeToVisit((String) item.getOrDefault("bestTimeToVisit", "Year-round"))
                        .recommendationReason((String) item.getOrDefault("recommendationReason", "Recommended based on your preferences"))
                        .suggestedActivities(activities.toArray(new String[0]))
                        .tags(tags.toArray(new String[0]))
                        .imageUrl((String) item.getOrDefault("imageUrl", "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1"))
                        .build();
            }).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse AI recommendations: {}", e.getMessage());
            return getFallbackRecommendations();
        }
    }

    private List<DestinationRecommendationDTO> getFallbackRecommendations() {
        return List.of(
            DestinationRecommendationDTO.builder()
                .destination("Manali")
                .description("A picturesque hill station in Himachal Pradesh with snow-capped mountains and adventure activities.")
                .currentWeather("12°C | Cold & Snowy")
                .weatherIcon("bi-snow")
                .estimatedBudgetMin("₹8,000")
                .estimatedBudgetMax("₹15,000")
                .bestTimeToVisit("October - June")
                .recommendationReason("Based on hill station preferences and cold weather affinity")
                .suggestedActivities(new String[]{"Snow trekking", "Rohtang Pass", "Mall Road shopping"})
                .tags(new String[]{"Hill Station", "Adventure", "Nature"})
                .imageUrl("https://images.unsplash.com/photo-1585409677983-0f6c41ca9c3b")
                .build(),
            DestinationRecommendationDTO.builder()
                .destination("Goa")
                .description("India's beach paradise with golden sands, vibrant nightlife, and Portuguese heritage.")
                .currentWeather("30°C | Sunny")
                .weatherIcon("bi-sun")
                .estimatedBudgetMin("₹10,000")
                .estimatedBudgetMax("₹20,000")
                .bestTimeToVisit("November - February")
                .recommendationReason("Popular beach destination matching your travel history")
                .suggestedActivities(new String[]{"Beach activities", "Water sports", "Church exploration"})
                .tags(new String[]{"Beach", "Family", "Culture"})
                .imageUrl("https://images.unsplash.com/photo-1512343879780-a9c9a1fd1c4e")
                .build(),
            DestinationRecommendationDTO.builder()
                .destination("Udaipur")
                .description("The City of Lakes with majestic palaces, rich heritage, and romantic boat rides.")
                .currentWeather("25°C | Pleasant")
                .weatherIcon("bi-cloud-sun")
                .estimatedBudgetMin("₹7,000")
                .estimatedBudgetMax("₹12,000")
                .bestTimeToVisit("September - March")
                .recommendationReason("Perfect for family vacations with cultural and scenic experiences")
                .suggestedActivities(new String[]{"Lake Pichola boat ride", "City Palace visit", "Folk dance shows"})
                .tags(new String[]{"Family", "Culture", "Romantic"})
                .imageUrl("https://images.unsplash.com/photo-1599661046289-e31897846e41")
                .build()
        );
    }
}
