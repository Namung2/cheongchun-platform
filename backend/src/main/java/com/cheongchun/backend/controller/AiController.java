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
    @PutMapping("/profile")
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
            return ResponseEntity.internalServerError()
                .body(-1L); // 오류 시에도 JSON 응답 반환
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
    
    // 대화 삭제
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            Authentication authentication,
            @PathVariable Long conversationId) {
        try {
            User user = getUserFromAuthentication(authentication);
            aiUserService.deleteConversation(user, conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("대화 삭제 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 사용자의 모든 대화 기록 조회
    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getUserConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Map<String, Object>> conversations = aiUserService.getUserConversations(user, page, size);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("대화 목록 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 비정규화 컬럼 활용 빠른 검색 API들 ===
    
    // 키워드로 대화 검색
    @GetMapping("/conversations/search")
    public ResponseEntity<List<Map<String, Object>>> searchConversations(
            Authentication authentication,
            @RequestParam String keyword) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Map<String, Object>> conversations = aiUserService.searchConversations(user, keyword);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("대화 검색 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 건강 관련 대화만 조회
    @GetMapping("/conversations/health")
    public ResponseEntity<List<Map<String, Object>>> getHealthConversations(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Map<String, Object>> conversations = aiUserService.getHealthConversations(user);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("건강 대화 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 스트레스 레벨별 대화 조회
    @GetMapping("/conversations/stress")
    public ResponseEntity<List<Map<String, Object>>> getHighStressConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int minStressLevel) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Map<String, Object>> conversations = aiUserService.getHighStressConversations(user, minStressLevel);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("스트레스 대화 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 감정 상태별 대화 조회
    @GetMapping("/conversations/mood/{mood}")
    public ResponseEntity<List<Map<String, Object>>> getConversationsByMood(
            Authentication authentication,
            @PathVariable String mood) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Map<String, Object>> conversations = aiUserService.getConversationsByMood(user, mood);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("감정별 대화 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return (User) userDetailsService.loadUserByUsername(username);
    }
}