package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.OAuth2UserInfo;
import com.cheongchun.backend.security.OAuth2UserInfoFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRegistrationService userRegistrationService;

    public CustomOAuth2UserService(SocialAccountRepository socialAccountRepository,
                                 UserRegistrationService userRegistrationService) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRegistrationService = userRegistrationService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            OAuth2Error oauth2Error = new OAuth2Error(
                "processing_error",
                "OAuth2 사용자 정보 처리 중 오류가 발생했습니다: " + ex.getMessage(),
                null
            );
            throw new OAuth2AuthenticationException(oauth2Error);
        }
    }


    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if(!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            OAuth2Error oauth2Error = new OAuth2Error(
                "email_not_found",
                "이메일 정보를 가져올 수 없습니다. " + registrationId.toUpperCase() + " 계정에서 이메일 제공 동의가 필요합니다.",
                null
            );
            throw new OAuth2AuthenticationException(oauth2Error);
        }

        SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
        Optional<SocialAccount> socialAccountOptional = socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());
        
        User user;
        if(socialAccountOptional.isPresent()) {
            user = socialAccountOptional.get().getUser();
            user = userRegistrationService.updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = userRegistrationService.registerNewUser(registrationId, oAuth2UserInfo);
        }

        return new CustomOAuth2User(
            oAuth2User, 
            user.getUsername(), 
            user.getId(), 
            user.getEmail(), 
            user.getName(), 
            user.getProviderType().name()
        );
    }

    public User createGoogleUser(String email, String name, String googleId) {
        return userRegistrationService.registerGoogleUser(email, name, googleId);
    }
}