package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.OAuth2UserInfo;
import com.cheongchun.backend.security.OAuth2UserInfoFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public CustomOAuth2UserService(UserRepository userRepository, SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if(!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
        Optional<SocialAccount> socialAccountOptional = socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());
        
        User user;
        if(socialAccountOptional.isPresent()) {
            user = socialAccountOptional.get().getUser();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return new CustomOAuth2User(oAuth2User, user.getUsername());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
        
        User user = new User();
        user.setProviderType(User.ProviderType.valueOf(registrationId.toUpperCase()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        user.setEmailVerified(true);
        
        // 중복되지 않는 username 생성
        String baseUsername = oAuth2UserInfo.getName() != null ? oAuth2UserInfo.getName() : "user";
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "");
        if(baseUsername.length() < 4) {
            baseUsername = "user" + baseUsername;
        }
        
        String username = baseUsername;
        int counter = 1;
        while(userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        user.setUsername(username);
        
        User savedUser = userRepository.save(user);
        
        // SocialAccount 생성
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(savedUser);
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setProviderEmail(oAuth2UserInfo.getEmail());
        socialAccount.setProviderName(oAuth2UserInfo.getName());
        socialAccount.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        
        socialAccountRepository.save(socialAccount);
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}