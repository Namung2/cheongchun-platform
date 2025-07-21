package com.cheongchun.config;

import com.cheongchun.backend.security.JwtAuthenticationFilter;
import com.cheongchun.backend.service.CustomOAuth2UserService;
import com.cheongchun.backend.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false))
                .authorizeHttpRequests(authorize -> authorize
                        // 기본 경로들
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // 정적 리소스 (Spring Boot 3.x 호환 패턴)
                        .requestMatchers("/static/**", "/public/**").permitAll()
                        .requestMatchers("/*.png", "/*.jpg", "/*.gif", "/*.css", "/*.js").permitAll()
                        .requestMatchers("/images/**", "/css/**", "/js/**").permitAll()

                        // API 엔드포인트들
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()  // 인증 관련
                        .requestMatchers("/oauth2/**").permitAll() // OAuth2 관련
                        .requestMatchers("/login/oauth2/**").permitAll()

                        // 개발 단계에서는 모든 API 허용
                        //.requestMatchers("/api/**").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect("/api/auth/oauth2/success");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("/api/auth/oauth2/failure?error="+ exception.getMessage());
                        })

                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}