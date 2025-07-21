package com.cheongchun.backend.service;

import com.cheongchun.backend.security.CustomOidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        try {
            // OAuth2UserRequest로 변환하여 기존 로직 재사용
            OAuth2User processedUser = customOAuth2UserService.processOAuth2User(userRequest, oidcUser);
            return new CustomOidcUser(oidcUser, ((com.cheongchun.backend.security.CustomOAuth2User) processedUser).getUsername());
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("oidc_processing_error", ex.getMessage(), null));
        }
    }
}