package com.cheongchun.backend.config;

import com.cheongchun.backend.security.JwtAuthenticationFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2Config oauth2Config;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          OAuth2Config oauth2Config) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2Config = oauth2Config;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 활성화 - WebConfig와 함께 작동
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 기본 경로들
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // 정적 리소스
                        .requestMatchers("/static/**", "/public/**").permitAll()
                        .requestMatchers("/*.png", "/*.jpg", "/*.gif", "/*.css", "/*.js").permitAll()
                        .requestMatchers("/images/**", "/css/**", "/js/**").permitAll()

                        // API 엔드포인트들
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()  // 인증 관련
                        .requestMatchers("/oauth2/**").permitAll() // OAuth2 관련

                        // 개발 단계에서는 모든 API 허용 (주석 해제 시)
                        // .requestMatchers("/api/**").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2Config::configureOAuth2Login)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 명시적으로 허용할 origin 설정
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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