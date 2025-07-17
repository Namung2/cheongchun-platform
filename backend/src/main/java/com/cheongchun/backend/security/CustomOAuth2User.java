package com.cheongchun.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    
    private OAuth2User oauth2User;
    private String username;

    public CustomOAuth2User(OAuth2User oauth2User, String username) {
        this.oauth2User = oauth2User;
        this.username = username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return this.username;
    }

    public String getUsername() {
        return this.username;
    }
}