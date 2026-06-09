package com.umaitpen.travelx.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String redirectUrl;
}
