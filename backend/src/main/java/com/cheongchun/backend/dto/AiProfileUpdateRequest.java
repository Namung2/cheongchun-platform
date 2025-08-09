package com.cheongchun.backend.dto;

import lombok.Data;

@Data
public class AiProfileUpdateRequest {
    private String ageGroup; // "65-70", "70-75", etc.
    private String healthProfile; // JSON string
    private String interests; // JSON string  
    private String conversationStyle; // "formal", "casual"
}