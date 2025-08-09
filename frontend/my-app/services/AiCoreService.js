import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

// Android 에뮬레이터에서는 10.0.2.2 사용
const AI_CORE_BASE_URL = __DEV__ && Platform.OS === 'android' 
  ? 'http://10.0.2.2:8001' 
  : 'http://localhost:8001';
// const AI_CORE_BASE_URL = 'https://your-ai-core-domain.com'; // 프로덕션 환경

class AiCoreService {
  constructor() {
    this.baseURL = AI_CORE_BASE_URL;
  }

  async getAuthHeaders() {
    const token = await AsyncStorage.getItem('accessToken');
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  }

  // 헬스 체크
  async checkHealth() {
    try {
      const response = await fetch(`${this.baseURL}/health`);
      return await response.json();
    } catch (error) {
      throw new Error(`AI Core 서비스에 연결할 수 없습니다: ${error.message}`);
    }
  }

  // REST API 채팅 (비스트리밍)
  async sendChatMessage(message, sessionId = null, userId) {
    try {
      const headers = await this.getAuthHeaders();
      
      const response = await fetch(`${this.baseURL}/chat`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          message,
          session_id: sessionId,
          user_id: userId
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      throw new Error(`채팅 메시지 전송 실패: ${error.message}`);
    }
  }

  // WebSocket 연결을 위한 URL 생성
  getWebSocketUrl(userId) {
    return `${this.baseURL.replace('http', 'ws')}/ws/chat/${userId}`;
  }

  // JWT 토큰 검증
  async verifyToken() {
    try {
      const headers = await this.getAuthHeaders();
      
      const response = await fetch(`${this.baseURL}/auth/verify`, {
        method: 'GET',
        headers
      });

      if (!response.ok) {
        throw new Error('Token verification failed');
      }

      return await response.json();
    } catch (error) {
      throw new Error(`토큰 검증 실패: ${error.message}`);
    }
  }
}

export default new AiCoreService();