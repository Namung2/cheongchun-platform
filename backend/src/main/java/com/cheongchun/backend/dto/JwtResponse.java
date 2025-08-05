package com.cheongchun.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Long id;
    private String username;
    private String email;
    private String name;

    public JwtResponse(String accessToken, Long id, String username, String email, String name) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
    }

    public JwtResponse(String accessToken, String refreshToken, Long expiresIn,
                       Long id, String username, String email, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
    }
}