package com.umaitpen.travelx.dto;

import com.umaitpen.travelx.enums.ProviderType;
import com.umaitpen.travelx.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String name;
    private String email;
    private String password;
    private Role role;
    private ProviderType providerType;
    private String providerCompany;
}
