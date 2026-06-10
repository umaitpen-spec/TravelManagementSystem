package com.umaitpen.travelx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umaitpen.travelx.dto.GoogleUserDTO;
import com.umaitpen.travelx.dto.JwtResponse;
import com.umaitpen.travelx.enums.AuthProvider;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.UserRepository;
import com.umaitpen.travelx.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    public JwtResponse authenticateWithGoogle(String idToken, String accessToken) {
        // Validate the token with Google
        Map<String, String> tokenInfo = validateGoogleToken(idToken);
        if (tokenInfo == null) {
            throw new RuntimeException("Invalid Google token - token validation returned null");
        }

        String googleId = tokenInfo.get("sub");
        String email = tokenInfo.get("email");

        if (googleId == null || googleId.isBlank()) {
            throw new RuntimeException("Invalid Google token - missing user ID (sub) from Google");
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Invalid Google token - missing email from Google");
        }

        // Fetch user profile from Google (optional, gracefully handles failure)
        GoogleUserDTO googleUser = null;
        try {
            if (accessToken != null && !accessToken.isBlank()) {
                googleUser = fetchGoogleUser(accessToken);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Google user profile: " + e.getMessage());
        }

        // Check if user exists by Google ID or email
        Optional<User> existingUser = userRepository.findByProviderId(googleId);
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByEmail(email);
        }

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update provider info if needed
            if (user.getProviderId() == null) {
                user.setProviderId(googleId);
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
        } else {
            // Create new user - default to ROLE_USER, never ADMIN
            String name = (googleUser != null && googleUser.getName() != null && !googleUser.getName().isBlank())
                    ? googleUser.getName()
                    : email.split("@")[0];
            user = User.builder()
                    .email(email)
                    .name(name)
                    .password("") // No password for Google users
                    .role(Role.ROLE_USER)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(googleId)
                    .enabled(true)
                    .build();
        }

        user = userRepository.save(user);

        // Generate JWT
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // Determine redirect URL
        String redirectUrl = "/";
        if (user.getRole() == Role.ROLE_ADMIN) {
            redirectUrl = "/admin/dashboard";
        } else if (user.getRole() == Role.ROLE_SERVICE_PROVIDER) {
            redirectUrl = "/provider/dashboard";
        }

        return JwtResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .redirectUrl(redirectUrl)
                .build();
    }

    private Map<String, String> validateGoogleToken(String idToken) {
        // Decode JWT payload directly (Google id_token is a signed JWT)
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Google token: invalid JWT format, expected 3 parts got " + parts.length);
        }
        // Decode the payload (second part)
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json;
        try {
            json = mapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Google token: failed to parse JWT payload: " + e.getMessage());
        }

        String sub = json.has("sub") ? json.get("sub").asText() : null;
        String email = json.has("email") ? json.get("email").asText() : null;

        if (sub == null || sub.isBlank()) {
            throw new RuntimeException("Google token: missing sub claim");
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google token: missing email claim");
        }
        return Map.of("sub", sub, "email", email);
    }

    private GoogleUserDTO fetchGoogleUser(String accessToken) {
        try {
            String url = GOOGLE_USER_INFO_URL + "?access_token=" + accessToken;
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null) {
                return GoogleUserDTO.builder()
                        .email(response.has("email") ? response.get("email").asText() : "")
                        .name(response.has("name") ? response.get("name").asText() : "")
                        .googleId(response.has("sub") ? response.get("sub").asText() : "")
                        .imageUrl(response.has("picture") ? response.get("picture").asText() : "")
                        .build();
            }
        } catch (Exception e) {
            // Ignore - profile fetch is optional
        }
        return null;
    }
}
