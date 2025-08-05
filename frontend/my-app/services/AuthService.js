// services/AuthService.js
// 인증 관련 로직을 담당하는 서비스 (기존 코드와 분리)

import AsyncStorage from '@react-native-async-storage/async-storage';
import apiService from './ApiService';

class AuthService {
  constructor() {
    this.currentUser = null;
    this.authListeners = [];
  }

  // ===== 사용자 상태 관리 =====

  // 현재 사용자 정보 반환
  getCurrentUser() {
    return this.currentUser;
  }

  // 로그인 상태 확인
  isAuthenticated() {
    return this.currentUser !== null;
  }

  // 인증 상태 변경 리스너 추가
  addAuthListener(listener) {
    this.authListeners.push(listener);
  }

  // 인증 상태 변경 리스너 제거
  removeAuthListener(listener) {
    this.authListeners = this.authListeners.filter(l => l !== listener);
  }

  // 인증 상태 변경 알림
  notifyAuthListeners(user) {
    this.currentUser = user;
    this.authListeners.forEach(listener => listener(user));
  }

  // ===== 앱 시작 시 인증 상태 복원 =====

  async initializeAuth() {
    try {
      const token = await AsyncStorage.getItem('accessToken');
      if (token) {
        // 토큰이 있으면 사용자 정보 조회
        const response = await apiService.getCurrentUser();
        if (response.success) {
          this.notifyAuthListeners(response.data);
          return response.data;
        } else {
          // 토큰이 유효하지 않으면 삭제
          await apiService.clearTokens();
        }
      }
      this.notifyAuthListeners(null);
      return null;
    } catch (error) {
      console.error('인증 초기화 실패:', error);
      await apiService.clearTokens();
      this.notifyAuthListeners(null);
      return null;
    }
  }

  // ===== 로그인 관련 =====

  // 이메일 로그인
  async loginWithEmail(email, password) {
    try {
      if (!email || !password) {
        throw new Error('이메일과 비밀번호를 입력해주세요');
      }

      const response = await apiService.login(email, password);
      
      if (response.success) {
        this.notifyAuthListeners(response.data.user);
        return {
          success: true,
          user: response.data.user,
          message: '로그인되었습니다',
        };
      } else {
        throw new Error(response.error?.message || '로그인에 실패했습니다');
      }
    } catch (error) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  // 회원가입
  async signUp(userData) {
    try {
      // 필수 필드 검증
      const requiredFields = ['email', 'password', 'name', 'age', 'location'];
      for (const field of requiredFields) {
        if (!userData[field]) {
          throw new Error(`${this.getFieldName(field)}을(를) 입력해주세요`);
        }
      }

      // 이메일 형식 검증
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(userData.email)) {
        throw new Error('올바른 이메일 형식을 입력해주세요');
      }

      // 비밀번호 강도 검증
      if (userData.password.length < 8) {
        throw new Error('비밀번호는 8자 이상이어야 합니다');
      }

      const response = await apiService.signup(userData);
      
      if (response.success) {
        this.notifyAuthListeners(response.data.user);
        return {
          success: true,
          user: response.data.user,
          message: '회원가입이 완료되었습니다',
        };
      } else {
        throw new Error(response.error?.message || '회원가입에 실패했습니다');
      }
    } catch (error) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  // 로그아웃
  async logout() {
    try {
      await apiService.logout();
      this.notifyAuthListeners(null);
      return {
        success: true,
        message: '로그아웃되었습니다',
      };
    } catch (error) {
      // 로그아웃은 에러가 나도 클라이언트에서는 성공으로 처리
      this.notifyAuthListeners(null);
      return {
        success: true,
        message: '로그아웃되었습니다',
      };
    }
  }

  // ===== 소셜 로그인 관련 =====

  // 카카오 로그인 URL 생성
  getKakaoLoginUrl() {
    return apiService.getSocialLoginUrl('kakao');
  }

  // 구글 로그인 URL 생성
  getGoogleLoginUrl() {
    return apiService.getSocialLoginUrl('google');
  }

  // 네이버 로그인 URL 생성  
  getNaverLoginUrl() {
    return apiService.getSocialLoginUrl('naver');
  }

  // 소셜 로그인 콜백 처리
  async handleSocialCallback(tokens) {
    try {
      if (tokens.accessToken) {
        await apiService.setToken(tokens.accessToken);
        if (tokens.refreshToken) {
          await apiService.setRefreshToken(tokens.refreshToken);
        }

        // 사용자 정보 조회
        const response = await apiService.getCurrentUser();
        if (response.success) {
          this.notifyAuthListeners(response.data);
          return {
            success: true,
            user: response.data,
            message: '로그인되었습니다',
          };
        }
      }
      
      throw new Error('소셜 로그인에 실패했습니다');
    } catch (error) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  // OAuth 코드를 토큰으로 교환
  async exchangeCodeForToken(code) {
    try {
      const response = await apiService.request('/auth/oauth/exchange', {
        method: 'POST',
        body: JSON.stringify({ code })
      });

      if (response.success && response.data.tokens) {
        await apiService.setToken(response.data.tokens.accessToken);
        await apiService.setRefreshToken(response.data.tokens.refreshToken);
        this.notifyAuthListeners(response.data.user);
        return {
          success: true,
          user: response.data.user,
          message: '로그인되었습니다'
        };
      } else {
        throw new Error(response.error?.message || 'OAuth 로그인에 실패했습니다');
      }
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  }

  // OAuth 성공 후 토큰과 사용자 정보로 인증 상태 업데이트
  async handleOAuthSuccess(token, userInfo) {
    try {
      // AsyncStorage에 토큰 저장 (이미 저장되어 있지만 확인차 재저장)
      await AsyncStorage.setItem('accessToken', token);
      
      // 사용자 정보를 AuthService 형식으로 변환하고 상태 업데이트
      const user = {
        id: userInfo.id,
        email: userInfo.email,
        name: userInfo.name,
        // 필요한 경우 추가 필드들
      };
      
      this.notifyAuthListeners(user);
      
      return {
        success: true,
        user: user,
        message: '로그인이 완료되었습니다'
      };
    } catch (error) {
      console.error('OAuth 성공 처리 오류:', error);
      return {
        success: false,
        error: error.message
      };
    }
  }

  // ===== 유틸리티 메서드 =====

  // 필드명을 한글로 변환
  getFieldName(field) {
    const fieldNames = {
      email: '이메일',
      password: '비밀번호',
      name: '이름',
      age: '나이',
      location: '지역',
      phone: '전화번호',
    };
    return fieldNames[field] || field;
  }

  // 비밀번호 강도 검사
  checkPasswordStrength(password) {
    const checks = {
      length: password.length >= 8,
      hasUpperCase: /[A-Z]/.test(password),
      hasLowerCase: /[a-z]/.test(password),
      hasNumbers: /\d/.test(password),
      hasNonalphas: /\W/.test(password),
    };

    const score = Object.values(checks).filter(Boolean).length;
    
    if (score < 2) return { strength: 'weak', message: '비밀번호가 너무 약합니다' };
    if (score < 4) return { strength: 'medium', message: '보통 강도의 비밀번호입니다' };
    return { strength: 'strong', message: '강한 비밀번호입니다' };
  }

  // ===== 서버 연결 상태 확인 =====

  async checkServerStatus() {
    try {
      await apiService.checkHealth();
      return { connected: true, message: '서버 연결 정상' };
    } catch (error) {
      return { connected: false, message: error.message };
    }
  }
}

// 싱글톤 인스턴스 생성
const authService = new AuthService();

export default authService;