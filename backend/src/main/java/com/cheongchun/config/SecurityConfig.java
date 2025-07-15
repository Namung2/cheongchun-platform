package com.cheongchun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()  // Actuator 엔드포인트는 인증 없이 접근
                        .requestMatchers("/test/**").permitAll()      // 테스트 엔드포인트도 허용
                        .anyRequest().authenticated()                 // 나머지는 인증 필요
                )
                .csrf(csrf -> csrf.disable())                     // 개발 시 CSRF 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())      // HTTP Basic 인증 비활성화
                .formLogin(form -> form.disable());              // 폼 로그인 비활성화

        return http.build();
    }
}