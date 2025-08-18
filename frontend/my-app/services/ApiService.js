// services/ApiService.js
// 기존 프론트엔드 코드를 건드리지 않고 새로 추가하는 API 서비스

import AsyncStorage from '@react-native-async-storage/async-storage';
import Config from '../config';

class ApiService {
  constructor() {
    // 프로덕션 환경 설정
    this.baseURL = Config.API.BASE_URL;
    this.timeout = Config.API.TIMEOUT;
  }

  // ===== 토큰 관리 =====
  
  async getToken() {
    try {
      return await AsyncStorage.getItem('accessToken');
    } catch (error) {
      console.error('토큰 조회 실패:', error);
      return null;
    }
  }

  async setToken(token) {
    try {
      await AsyncStorage.setItem('accessToken', token);
    } catch (error) {
      console.error('토큰 저장 실패:', error);
    }
  }

  async getRefreshToken() {
    try {
      return await AsyncStorage.getItem('refreshToken');
    } catch (error) {
      console.error('리프레시 토큰 조회 실패:', error);
      return null;
    }
  }

  async setRefreshToken(token) {
    try {
      await AsyncStorage.setItem('refreshToken', token);
    } catch (error) {
      console.error('리프레시 토큰 저장 실패:', error);
    }
  }

  async clearTokens() {
    try {
      await AsyncStorage.multiRemove(['accessToken', 'refreshToken']);
    } catch (error) {
      console.error('토큰 삭제 실패:', error);
    }
  }

  // ===== 공통 API 호출 메서드 =====

  async request(endpoint, options = {}) {
    const config = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: this.timeout,
      ...options,
    };

    // 인증 토큰 추가
    const token = await this.getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), this.timeout);

      const response = await fetch(`${this.baseURL}${endpoint}`, {
        ...config,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      // 응답이 비어있는지 확인
      const responseText = await response.text();
      let data;
      
      if (responseText) {
        try {
          data = JSON.parse(responseText);
        } catch (parseError) {
          throw new Error('잘못된 JSON 응답');
        }
      } else {
        // 빈 응답인 경우
        if (response.ok) {
          data = null;
        } else {
          throw new Error(`서버 오류: ${response.status}`);
        }
      }

      // 토큰 만료 시 자동 갱신 시도
      if (response.status === 401 && data.error?.code === 'TOKEN_EXPIRED') {
        const refreshSuccess = await this.refreshToken();
        if (refreshSuccess) {
          // 토큰 갱신 후 재시도
          const newToken = await this.getToken();
          config.headers['Authorization'] = `Bearer ${newToken}`;
          
          const retryResponse = await fetch(`${this.baseURL}${endpoint}`, config);
          const retryText = await retryResponse.text();
          
          if (retryText) {
            return JSON.parse(retryText);
          } else {
            return retryResponse.ok ? null : { error: `서버 오류: ${retryResponse.status}` };
          }
        } else {
          // 리프레시 실패 시 로그아웃 처리
          await this.clearTokens();
          throw new Error('로그인이 필요합니다');
        }
      }

      return data;
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new Error('요청 시간이 초과되었습니다');
      }
      throw new Error(`API 호출 실패: ${error.message}`);
    }
  }

  // ===== 토큰 갱신 =====

  async refreshToken() {
    const refreshToken = await this.getRefreshToken();
    if (!refreshToken) {
      return false;
    }

    try {
      const response = await fetch(`${this.baseURL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      const data = await response.json();
      
      if (data.success && data.data.tokens) {
        await this.setToken(data.data.tokens.accessToken);
        await this.setRefreshToken(data.data.tokens.refreshToken);
        return true;
      }
      
      return false;
    } catch (error) {
      console.error('토큰 갱신 실패:', error);
      return false;
    }
  }

  // ===== 인증 관련 API =====

  // 이메일 회원가입
  async signup(userData) {
    return await this.request('/auth/signup', {
      method: 'POST',
      body: JSON.stringify({
        email: userData.email,
        password: userData.password,
        name: userData.name,
        age: userData.age,
        gender: userData.gender || 'MALE',
        location: userData.location,
        phone: userData.phone,
        agreementTerms: true,
        agreementPrivacy: true,
        agreementMarketing: userData.agreementMarketing || false,
      }),
    });
  }

  // 이메일 로그인
  async login(email, password) {
    const response = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    // 로그인 성공 시 토큰 저장
    if (response.success && response.data.tokens) {
      await this.setToken(response.data.tokens.accessToken);
      await this.setRefreshToken(response.data.tokens.refreshToken);
    }

    return response;
  }

  // 로그아웃
  async logout() {
    try {
      await this.request('/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('로그아웃 API 호출 실패:', error);
    } finally {
      await this.clearTokens();
    }
  }

  // 현재 사용자 정보 조회
  async getCurrentUser() {
    return await this.request('/auth/me');
  }

  // ===== 사용자 프로필 API =====

  // 프로필 조회
  async getProfile() {
    return await this.request('/users/profile');
  }

  // 프로필 업데이트
  async updateProfile(profileData) {
    return await this.request('/users/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  }

  // ===== 모임 관련 API =====

  // 모임 목록 조회
  async getMeetings(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = `/meetings${queryString ? `?${queryString}` : ''}`;
    return await this.request(endpoint);
  }

  // 오늘의 베스트 모임
  async getTodayBestMeetings() {
    return await this.request('/meetings/today-best');
  }

  // 모임 상세 조회
  async getMeetingDetail(meetingId) {
    return await this.request(`/meetings/${meetingId}`);
  }

  // 모임 참여 신청
  async joinMeeting(meetingId) {
    return await this.request(`/meetings/${meetingId}/join`, {
      method: 'POST',
    });
  }

  // ===== 찜 기능 API =====

  // 찜 목록 조회
  async getWishlist(page = 0, size = 20) {
    return await this.request(`/wishlist?page=${page}&size=${size}`);
  }

  // 찜 추가
  async addToWishlist(meetingId) {
    return await this.request(`/wishlist/${meetingId}`, {
      method: 'POST',
    });
  }

  // 찜 삭제
  async removeFromWishlist(meetingId) {
    return await this.request(`/wishlist/${meetingId}`, {
      method: 'DELETE',
    });
  }

  // ===== 소셜 로그인 관련 =====

  // 소셜 로그인 URL 생성
  getSocialLoginUrl(provider) {
    return `https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/oauth2/authorization/${provider}`;
  }

  // ===== AI 채팅 관련 API =====

  // 대화 저장
  async saveConversation(conversationData) {
    return await this.request('/ai/conversation', {
      method: 'POST',
      body: JSON.stringify(conversationData),
    });
  }

  // 채팅 히스토리 조회
  async getChatHistory(userId, limit = 10) {
    return await this.request(`/ai/history/${userId}?limit=${limit}`);
  }

  // AI 사용자 인사이트 조회
  async getUserInsights(userId) {
    return await this.request(`/ai/insights/${userId}`);
  }

  // AI 프로필 조회
  async getAiProfile() {
    return await this.request('/ai/profile');
  }

  // AI 프로필 업데이트
  async updateAiProfile(profileData) {
    return await this.request('/ai/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  }

  // 건강 요약 조회
  async getHealthSummary() {
    return await this.request('/ai/health-summary');
  }

  // ===== 헬스체크 =====

  async checkHealth() {
    try {
      const response = await fetch('https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/actuator/health');
      return await response.json();
    } catch (error) {
      throw new Error('서버에 연결할 수 없습니다');
    }
  }
}

// 싱글톤 인스턴스 생성
const apiService = new ApiService();

export default apiService;