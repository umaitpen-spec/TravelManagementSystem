package com.umaitpen.travelx.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserDTO {
    private String email;
    private String name;
    private String googleId;
    private String imageUrl;
}
