package com.cheongchun.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 기본 테스트 API
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "안녕하세요! 청춘장터 백엔드입니다! 🎉");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    /**
     * 간단한 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "cheongchun-backend");
        response.put("status", "RUNNING");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 경로 변수 테스트
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTest(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "사용자 ID " + userId + " 요청 처리됨");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}