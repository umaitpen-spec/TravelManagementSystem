package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.JwtResponse;
import com.umaitpen.travelx.dto.UserDTO;
import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import com.umaitpen.travelx.security.JwtUtil;
import com.umaitpen.travelx.service.UserService;
import com.umaitpen.travelx.service.impl.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final GoogleAuthService googleAuthService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDto) {
        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .password(userDto.getPassword())
                .role(userDto.getRole() != null ? userDto.getRole() : Role.ROLE_USER)
                .providerType(userDto.getProviderType())
                .providerCompany(userDto.getProviderCompany())
                .build();
        User saved = userService.register(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String registerForm(@RequestParam("name") String name,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam(value = "role", defaultValue = "ROLE_USER") String role,
                               @RequestParam(value = "providerType", required = false) String providerType,
                               @RequestParam(value = "providerCompany", required = false) String providerCompany,
                               RedirectAttributes redirectAttributes) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(Role.valueOf(role))
                .providerType(providerType != null ? ProviderType.valueOf(providerType) : null)
                .providerCompany(providerCompany)
                .enabled(true)
                .build();
        try {
            userService.register(user);
            redirectAttributes.addFlashAttribute("regSuccess", "Account created. Please sign in.");
            return "redirect:/login";
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("registerError", "An account with this email already exists.");
            return "redirect:/register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User found = userService.findByEmail(username);
        if (found == null || !passwordEncoder.matches(password, found.getPassword())) {
            redirectAttributes.addFlashAttribute("loginError", "Invalid credentials");
            return "redirect:/login?error";
        }
        session.setAttribute("userId", found.getId());
        session.setAttribute("userName", found.getName());

        // Set Spring Security authentication so session-based auth works
        Authentication auth = new UsernamePasswordAuthenticationToken(
            found, null,
            Collections.singletonList(new SimpleGrantedAuthority(found.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        redirectAttributes.addFlashAttribute("welcomeMessage", "Welcome, " + found.getName() + "!");
        return "redirect:/";
    }

    @PostMapping("/google")
    @ResponseBody
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        String accessToken = body.get("accessToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Google ID token"));
        }
        try {
            JwtResponse jwtResponse = googleAuthService.authenticateWithGoogle(idToken, accessToken);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/session-google")
    public String sessionGoogle(@RequestBody Map<String, String> body, HttpSession session) {
        String token = body.get("token");
        if (token == null || !jwtUtil.validateToken(token)) {
            return "redirect:/login?error";
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String email = jwtUtil.getEmailFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        session.setAttribute("userId", userId);
        session.setAttribute("userEmail", email);
        session.setAttribute("userName", email.split("@")[0]);
        session.setAttribute("userRole", role);

        // Set Spring Security authentication so session-based auth works for subsequent requests
        Authentication auth = new UsernamePasswordAuthenticationToken(
            userId, null,
            Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/";
    }
}
