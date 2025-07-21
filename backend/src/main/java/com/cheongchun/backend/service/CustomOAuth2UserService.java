package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.CustomOidcUser;
import com.cheongchun.backend.security.OAuth2UserInfo;
import com.cheongchun.backend.security.OAuth2UserInfoFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OidcUserService oidcUserService = new OidcUserService();

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
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("oauth2_processing_error", ex.getMessage(), null));
        }
    }


    public OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if(!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("email_not_found", "Email not found from OAuth2 provider", null));
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
        
        // 같은 이메일로 이미 가입된 사용자가 있는지 확인
        Optional<User> existingUserOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        
        User user;
        if (existingUserOptional.isPresent()) {
            // 같은 이메일로 이미 가입된 계정이 있으면 예외 발생
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("already_registered", "이미 가입된 계정이 있습니다.", null));
        } else {
            // 새 사용자 생성
            user = new User();
            user.setProviderType(User.ProviderType.valueOf(registrationId.toUpperCase()));
            user.setProviderId(oAuth2UserInfo.getId());
            user.setName(oAuth2UserInfo.getName());
            user.setEmail(oAuth2UserInfo.getEmail());
            user.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
            user.setEmailVerified(true);
            
            // 중복되지 않는 username 생성 (새 사용자인 경우에만)
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
            
            user = userRepository.save(user);
        }
        
        // SocialAccount 생성
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setProviderEmail(oAuth2UserInfo.getEmail());
        socialAccount.setProviderName(oAuth2UserInfo.getName());
        socialAccount.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        
        socialAccountRepository.save(socialAccount);
        
        return user;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        // Hibernate proxy 문제 방지를 위해 필드들에 명시적 접근
        existingUser.getId(); // id 로드
        existingUser.getUsername(); // username 로드
        existingUser.getEmail(); // email 로드
        existingUser.getProviderType(); // providerType 로드
        
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}