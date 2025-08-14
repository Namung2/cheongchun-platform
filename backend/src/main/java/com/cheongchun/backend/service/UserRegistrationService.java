package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.OAuth2UserInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UsernameGeneratorService usernameGeneratorService;

    public UserRegistrationService(UserRepository userRepository,
                                 SocialAccountRepository socialAccountRepository,
                                 UsernameGeneratorService usernameGeneratorService) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.usernameGeneratorService = usernameGeneratorService;
    }

    public User registerNewUser(String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        // 안전장치: try-catch로 중복 키 에러 처리
        try {
            SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
            
            User user = createUser(oAuth2UserInfo, User.ProviderType.valueOf(registrationId.toUpperCase()));
            User savedUser = userRepository.save(user);
            
            createSocialAccount(savedUser, provider, oAuth2UserInfo);
            
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            // 중복 키 에러 발생 시 기존 사용자 반환
            Optional<User> existingUser = userRepository.findByEmail(oAuth2UserInfo.getEmail());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            throw e;
        }
    }

    public User registerGoogleUser(String email, String name, String googleId) {
        // 안전장치: try-catch로 중복 키 에러 처리
        try {
            User user = createGoogleUser(email, name, googleId);
            User savedUser = userRepository.save(user);
            
            createGoogleSocialAccount(savedUser, email, name, googleId);
            
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            // 중복 키 에러 발생 시 기존 사용자 반환
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            throw e;
        }
    }

    public User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }

    private void validateEmailNotExists(String email) {
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            // 구글 로그인의 경우 기존 계정이 있으면 예외 발생하지 않고 그대로 사용
            // 다른 소셜 로그인도 동일하게 처리할 수 있도록 개선 필요
            OAuth2Error oauth2Error = new OAuth2Error(
                "email_already_exists",
                "이미 가입된 계정이 존재합니다. 기존 로그인 방법을 사용해주세요.",
                null
            );
            throw new OAuth2AuthenticationException(oauth2Error);
        }
    }

    private User createUser(OAuth2UserInfo oAuth2UserInfo, User.ProviderType providerType) {
        User user = new User();
        user.setProviderType(providerType);
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        user.setEmailVerified(true);
        user.setUsername(usernameGeneratorService.generateUniqueUsername(oAuth2UserInfo.getName()));
        
        return user;
    }

    private User createGoogleUser(String email, String name, String googleId) {
        User user = new User();
        user.setProviderType(User.ProviderType.GOOGLE);
        user.setProviderId(googleId);
        user.setName(name);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setUsername(usernameGeneratorService.generateUniqueUsername(name));
        
        return user;
    }

    private void createSocialAccount(User user, SocialAccount.Provider provider, OAuth2UserInfo oAuth2UserInfo) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setProviderEmail(oAuth2UserInfo.getEmail());
        socialAccount.setProviderName(oAuth2UserInfo.getName());
        socialAccount.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        
        socialAccountRepository.save(socialAccount);
    }

    private void createGoogleSocialAccount(User user, String email, String name, String googleId) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(SocialAccount.Provider.GOOGLE);
        socialAccount.setProviderId(googleId);
        socialAccount.setProviderEmail(email);
        socialAccount.setProviderName(name);
        
        socialAccountRepository.save(socialAccount);
    }
}