package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.AiProfileUpdateRequest;
import com.cheongchun.backend.dto.AiConversationRequest;
import com.cheongchun.backend.dto.AiUserInsightResponse;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.AiUserService;
import com.cheongchun.backend.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {
    
    private final AiUserService aiUserService;
    private final UserDetailsServiceImpl userDetailsService;
    
    // 사용자 AI 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserAiProfile(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Map<String, Object> profile = aiUserService.getUserAiProfile(user);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("AI 프로필 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 사용자 AI 프로필 업데이트
    @PUT("/profile")
    public ResponseEntity<Void> updateUserAiProfile(
            Authentication authentication,
            @RequestBody AiProfileUpdateRequest request) {
        try {
            User user = getUserFromAuthentication(authentication);
            aiUserService.updateUserAiProfile(user, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("AI 프로필 업데이트 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 대화 저장 (AI Core에서 호출)
    @PostMapping("/conversation")
    public ResponseEntity<Long> saveConversation(@RequestBody AiConversationRequest request) {
        try {
            Long conversationId = aiUserService.saveConversation(request);
            return ResponseEntity.ok(conversationId);
        } catch (Exception e) {
            log.error("대화 저장 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 사용자 인사이트 조회
    @GetMapping("/insights/{userId}")
    public ResponseEntity<AiUserInsightResponse> getUserInsights(@PathVariable Long userId) {
        try {
            AiUserInsightResponse insights = aiUserService.getUserInsights(userId);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("사용자 인사이트 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 최근 대화 히스토리 조회 (AI Core에서 사용)
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> history = aiUserService.getChatHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("채팅 히스토리 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 건강 관련 대화 요약
    @GetMapping("/health-summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Map<String, Object> summary = aiUserService.getHealthSummary(user);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("건강 요약 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return (User) userDetailsService.loadUserByUsername(username);
    }
}