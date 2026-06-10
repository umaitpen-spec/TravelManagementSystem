package com.umaitpen.travelx.config;

import com.umaitpen.travelx.enums.AuthProvider;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;

    private Long extractUserId(OAuth2User oauth2User) {
        Object userIdAttr = oauth2User.getAttributes().get("userId");
        if (userIdAttr != null) {
            return ((Number) userIdAttr).longValue();
        }
        return null;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        System.out.println("=== CustomAuthenticationSuccessHandler FIRED ===");
        System.out.println("Principal type: " + authentication.getPrincipal().getClass().getName());
        System.out.println("Authorities: " + authentication.getAuthorities());

        String email = null;
        Long userId = null;
        String userName = null;

        Object principal = authentication.getPrincipal();

        // Try to get user from DB - first by email, then by providerId (google sub)
        Map<String, Object> attrs = null;
        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser oidcUser) {
            attrs = oidcUser.getAttributes();
        } else if (principal instanceof OAuth2User oauth2User) {
            attrs = oauth2User.getAttributes();
        }

        if (attrs != null) {
            email = (String) attrs.get("email");
            userName = (String) attrs.get("name");
            String googleId = (String) attrs.get("sub");

            System.out.println("=== OAuth attrs: email=" + email + ", googleId=" + googleId);

            // Search DB for user
            try {
                // Try by providerId first (most reliable)
                if (googleId != null) {
                    User dbUser = userRepository.findByProviderId(googleId).orElse(null);
                    if (dbUser != null) {
                        userId = dbUser.getId();
                        userName = dbUser.getName();
                        email = dbUser.getEmail();
                        System.out.println("=== SuccessHandler found user by providerId: id=" + userId);
                    }
                }
                // Fallback by email
                if (userId == null && email != null) {
                    User dbUser = userRepository.findByEmail(email).orElse(null);
                    if (dbUser != null) {
                        userId = dbUser.getId();
                        userName = dbUser.getName();
                        System.out.println("=== SuccessHandler found user by email: id=" + userId);
                    }
                }
                // Create user if not found - this handles case where CustomOAuth2UserService was skipped
                if (userId == null && email != null && googleId != null) {
                    System.out.println("=== Creating new user in success handler ===");
                    User newUser = User.builder()
                        .email(email)
                        .name(userName != null ? userName : email.split("@")[0])
                        .password("")
                        .role(Role.ROLE_USER)
                        .authProvider(AuthProvider.GOOGLE)
                        .providerId(googleId)
                        .enabled(true)
                        .build();
                    newUser = userRepository.save(newUser);
                    userId = newUser.getId();
                    System.out.println("=== Created user id=" + userId);
                }
            } catch (Exception e) {
                System.out.println("DB lookup failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Also check GoogleOAuth2User wrapper
        if (principal instanceof GoogleOAuth2User googleUser) {
            userId = googleUser.getUser().getId();
            email = googleUser.getUser().getEmail();
            userName = googleUser.getUser().getName();
        } else if (principal instanceof OAuth2User oauth2User) {
            email = (String) oauth2User.getAttributes().get("email");
            userId = extractUserId(oauth2User);
            userName = (String) oauth2User.getAttributes().get("name");
        } else {
            email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            userId = user != null ? user.getId() : null;
            userName = user != null ? user.getName() : null;
        }

        if (userName == null && email != null) {
            userName = email.split("@")[0];
        }

        // Save session attributes BEFORE redirect to avoid new-session problem
        HttpSession session = request.getSession();
        session.setAttribute("userId", userId);
        session.setAttribute("userName", userName);
        session.setAttribute("userEmail", email);

        System.out.println("=== SuccessHandler userId=" + userId + " email=" + email);
        System.out.println("=== SuccessHandler session id=" + session.getId() + " userId=" + session.getAttribute("userId"));

        String redirectUrl = "/";
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            redirectUrl = "/admin/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SERVICE_PROVIDER"))) {
            redirectUrl = "/provider/dashboard";
        }

        // Use forward to preserve session - redirect creates new session
        request.getRequestDispatcher(redirectUrl).forward(request, response);
    }
}
