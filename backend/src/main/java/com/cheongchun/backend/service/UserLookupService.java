package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.OAuth2UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 조회 전담 서비스
 * SRP: 사용자 존재 여부 확인과 조회만 담당
 */
@Service
@Transactional(readOnly = true)
public class UserLookupService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserLookupService(UserRepository userRepository, 
                           SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * 소셜 계정으로 사용자 조회
     */
    public Optional<User> findBySocialAccount(SocialAccount.Provider provider, String providerId) {
        return socialAccountRepository.findByProviderAndProviderId(provider, providerId)
                .map(SocialAccount::getUser);
    }

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    /**
     * OAuth2 사용자 정보로 기존 사용자 조회
     * 1. 소셜 계정으로 먼저 조회
     * 2. 없으면 이메일로 조회
     */
    public Optional<User> findExistingUser(SocialAccount.Provider provider, OAuth2UserInfo userInfo) {
        // 1. 소셜 계정으로 조회
        Optional<User> socialUser = findBySocialAccount(provider, userInfo.getId());
        if (socialUser.isPresent()) {
            return socialUser;
        }

        // 2. 이메일로 조회
        return findByEmail(userInfo.getEmail());
    }

    /**
     * 사용자 존재 여부 확인
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * 소셜 계정 존재 여부 확인
     */
    public boolean existsBySocialAccount(SocialAccount.Provider provider, String providerId) {
        return findBySocialAccount(provider, providerId).isPresent();
    }
}