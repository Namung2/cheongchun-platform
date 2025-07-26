package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.JwtResponse;
import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public JwtResponse registerUser(SignUpRequest signUpRequest, HttpServletRequest request) {
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
        user.setRole(User.Role.USER);           //기본값
        user.setEmailVerified(false);
        User savedUser = userRepository.save(user);

        // 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signUpRequest.getUsername(),
                        signUpRequest.getPassword()
                )
        );

        return generateTokenResponse(savedUser, request);
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        return generateTokenResponse(user, request);
    }

    @Transactional
    public JwtResponse refreshToken(String refreshTokenStr, HttpServletRequest request) {
        // 1. 리프레시 토큰 조회 및 검증
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 리프레시 토큰입니다"));

        // 2. 토큰 만료 및 유효성 검증
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        // 3. 기존 토큰 사용 처리 (one-time use)
        refreshTokenService.markTokenAsUsed(refreshToken);

        // 4. 새로운 토큰 쌍 생성
        User user = refreshToken.getUser();
        return generateTokenResponse(user, request);
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
     * 토큰 응답 생성 (AccessToken + RefreshToken)
     */
    private JwtResponse generateTokenResponse(User user, HttpServletRequest request) {
        // Access Token 생성
        String accessToken = jwtUtil.generateTokenFromUsername(user.getUsername());

        // Refresh Token 생성
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

        return new JwtResponse(
                accessToken,
                refreshToken.getToken(),
                jwtUtil.getExpirationDateFromToken(accessToken).getTime(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName()
        );
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