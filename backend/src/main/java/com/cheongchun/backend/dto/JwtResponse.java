package com.cheongchun.backend.dto;

public class JwtResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // 만료 시간 (타임스탬프)
    private Long id;
    private String username;
    private String email;
    private String name;

    // 기존 생성자 (하위 호환성)
    public JwtResponse(String accessToken, Long id, String username, String email, String name) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
    }

    // 새로운 생성자 (리프레시 토큰 포함)
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

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}