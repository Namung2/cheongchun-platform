package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    // 쿠키 기반 사용자 생성 (토큰 없이)
    public User createUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("사용자명이 이미 사용 중입니다!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("이메일이 이미 사용 중입니다!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setName(signUpRequest.getName());
        user.setProviderType(User.ProviderType.LOCAL);
        user.setRole(User.Role.USER);
        user.setEmailVerified(false);
        
        return userRepository.save(user);
    }


    // 쿠키 기반 사용자 인증 (토큰 없이)
    public User authenticateUserCookie(LoginRequest loginRequest) {
        // 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        // 인증된 사용자 객체 반환
        User authenticatedUser = (User) authentication.getPrincipal();
        return authenticatedUser;
    }



    @Transactional
    public void logout(User user, String refreshTokenStr) {
        // 특정 리프레시 토큰만 무효화 (다른 디바이스 로그인 유지)
        if (refreshTokenStr != null && !refreshTokenStr.trim().isEmpty()) {
            refreshTokenService.revokeToken(refreshTokenStr);
        } else {
            // 리프레시 토큰이 없으면 해당 사용자의 모든 토큰 무효화
            refreshTokenService.revokeAllTokensByUser(user);
        }
    }

    @Transactional
    public void logoutFromAllDevices(User user) {
        // 모든 디바이스에서 로그아웃
        refreshTokenService.revokeAllTokensByUser(user);
    }


    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 사용자 활성 세션 조회
     */
    @Transactional(readOnly = true)
    public RefreshTokenService.RefreshTokenStats getUserTokenStats(User user) {
        return refreshTokenService.getTokenStats(user);
    }
}