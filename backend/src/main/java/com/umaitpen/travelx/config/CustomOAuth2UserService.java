package com.umaitpen.travelx.config;

import com.umaitpen.travelx.enums.AuthProvider;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        System.out.println("=== CustomOAuth2UserService.loadUser() CALLED ===");
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_oauth2_flow", "Missing Google user info", null));
        }

        // Find or create user
        log.debug("Google OAuth - googleId: {}, email: {}, name: {}", googleId, email, name);
        Optional<User> byProviderId = userRepository.findByProviderId(googleId);
        User user;
        if (byProviderId.isPresent()) {
            log.debug("Found user by providerId: {}", byProviderId.get().getId());
            user = byProviderId.get();
        } else {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                log.debug("Found user by email, linking providerId: {}", byEmail.get().getId());
                user = byEmail.get();
                user.setProviderId(googleId);
                user.setAuthProvider(AuthProvider.GOOGLE);
            } else {
                log.debug("Creating new user for Google OAuth: {}", email);
                user = User.builder()
                        .email(email)
                        .name(name != null ? name : email.split("@")[0])
                        .password("")
                        .role(Role.ROLE_USER)
                        .authProvider(AuthProvider.GOOGLE)
                        .providerId(googleId)
                        .enabled(true)
                        .build();
            }
        }

        log.debug("Saving user: {}", user.getEmail());
        user = userRepository.save(user);
        entityManager.flush(); // Force immediate write to DB
        System.out.println("=== CustomOAuth2UserService: saved user id=" + user.getId() + " email=" + user.getEmail());
        log.debug("User saved with id: {}", user.getId());
        attributes.put("userId", user.getId());
        return new GoogleOAuth2User(user, attributes);
    }
}
