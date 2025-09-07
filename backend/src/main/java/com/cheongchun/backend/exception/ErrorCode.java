package com.cheongchun.backend.exception;

public enum ErrorCode {
    // User related errors
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_002", "사용자명이 이미 사용 중입니다"),
    EMAIL_ALREADY_EXISTS("USER_003", "이메일이 이미 사용 중입니다"),
    INVALID_USER_CREDENTIALS("USER_004", "잘못된 사용자 인증 정보입니다"),
    
    // Authentication related errors
    INVALID_TOKEN("AUTH_001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN("AUTH_002", "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND("AUTH_003", "리프레시 토큰을 찾을 수 없습니다"),
    INVALID_REFRESH_TOKEN("AUTH_004", "유효하지 않은 리프레시 토큰입니다"),
    
    // Meeting related errors
    MEETING_NOT_FOUND("MEETING_001", "미팅을 찾을 수 없습니다"),
    MEETING_FULL("MEETING_002", "미팅이 가득 찼습니다"),
    ALREADY_JOINED_MEETING("MEETING_003", "이미 참여한 미팅입니다"),
    NOT_MEETING_PARTICIPANT("MEETING_004", "미팅 참여자가 아닙니다"),
    
    // Wishlist related errors
    WISHLIST_NOT_FOUND("WISHLIST_001", "위시리스트를 찾을 수 없습니다"),
    
    // AI related errors
    AI_SERVICE_ERROR("AI_001", "AI 서비스 오류가 발생했습니다"),
    
    // System errors
    INTERNAL_SERVER_ERROR("SYS_001", "내부 서버 오류가 발생했습니다"),
    INVALID_INPUT("SYS_002", "잘못된 입력입니다");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}