package com.ecommerce.user.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String roles; // e.g. "ROLE_USER"
}
