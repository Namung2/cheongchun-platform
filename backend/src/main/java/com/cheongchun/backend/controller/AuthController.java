package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.dto.response.ApiResponse;
import com.cheongchun.backend.dto.response.UserResponse;
import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.mapper.UserMapper;
import com.cheongchun.backend.service.AuthService;
import com.cheongchun.backend.service.RefreshTokenService;
import com.cheongchun.backend.util.JwtUtil;
import com.cheongchun.backend.util.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserMapper userMapper) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    /**
     * 이메일 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        try {
            // 사용자 등록 및 토큰 생성
            User newUser = authService.createUser(signUpRequest);
            
            // JWT 토큰 생성
            String jwt = jwtUtil.generateTokenFromUsername(newUser.getEmail());
            
            // 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = ControllerUtils.getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser, userAgent, ipAddress);
            
            // 쿠키에 토큰 설정
            setAuthCookies(response, jwt, refreshToken.getToken());

            // DTO를 사용한 응답
            UserResponse userResponse = userMapper.toBasicUserResponse(newUser);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "회원가입이 완료되었습니다");

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("회원가입 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "SIGNUP_FAILED", 
                e.getMessage(), 
                "회원가입 중 오류가 발생했습니다"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request, HttpServletResponse response) {
        try {
            // 사용자 인증
            User authenticatedUser = authService.authenticateUserCookie(loginRequest);
            
            // JWT 토큰 생성
            String jwt = jwtUtil.generateTokenFromUsername(authenticatedUser.getEmail());
            
            // 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = ControllerUtils.getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authenticatedUser, userAgent, ipAddress);
            
            // 쿠키에 토큰 설정
            setAuthCookies(response, jwt, refreshToken.getToken());

            UserResponse userResponse = userMapper.toUserResponse(authenticatedUser);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "로그인이 완료되었습니다");

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("로그인 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "LOGIN_FAILED", 
                "로그인에 실패했습니다", 
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                
                UserResponse userResponse = userMapper.toUserResponse(user);
                ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "사용자 정보 조회 성공");

                return ResponseEntity.ok(apiResponse);
            } else {
                ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                    "USER_INFO_ERROR", 
                    "사용자 정보 조회 실패", 
                    "인증되지 않은 사용자입니다"
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("사용자 정보 조회 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "USER_INFO_ERROR", 
                "사용자 정보 조회 중 오류가 발생했습니다", 
                e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        ApiResponse<String> response = ApiResponse.success("cheongchun-backend", "인증 API가 정상적으로 작동하고 있습니다");
        return ResponseEntity.ok(response);
    }

    // Helper methods
    
    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // JWT를 HttpOnly 쿠키로 설정 (7일)
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true); // HTTPS에서만 전송
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(jwtCookie);

        // 리프레시 토큰을 HttpOnly 쿠키로 설정 (7일)
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(refreshCookie);
    }

}