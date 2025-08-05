// hooks/useAuth.js
// React Native에서 인증 상태를 관리하는 커스텀 훅

import { useState, useEffect, useCallback } from 'react';
import authService from '../services/AuthService';

export function useAuth() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 인증 상태 변경 리스너
  useEffect(() => {
    const handleAuthChange = (userData) => {
      setUser(userData);
      setLoading(false);
      setError(null);
    };

    // 리스너 등록
    authService.addAuthListener(handleAuthChange);

    // 앱 시작 시 인증 상태 복원
    const initAuth = async () => {
      try {
        await authService.initializeAuth();
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    };

    initAuth();

    // 컴포넌트 언마운트 시 리스너 정리
    return () => {
      authService.removeAuthListener(handleAuthChange);
    };
  }, []);

  // 이메일 로그인
  const loginWithEmail = useCallback(async (email, password) => {
    setLoading(true);
    setError(null);
    
    const result = await authService.loginWithEmail(email, password);
    
    setLoading(false);
    if (!result.success) {
      setError(result.error);
    }
    
    return result;
  }, []);

  // 회원가입
  const signUp = useCallback(async (userData) => {
    setLoading(true);
    setError(null);
    
    const result = await authService.signUp(userData);
    
    setLoading(false);
    if (!result.success) {
      setError(result.error);
    }
    
    return result;
  }, []);

  // 로그아웃
  const logout = useCallback(async () => {
    setLoading(true);
    const result = await authService.logout();
    setLoading(false);
    return result;
  }, []);

  // 소셜 로그인 URL 가져오기
  const getSocialLoginUrl = useCallback((provider) => {
    switch (provider) {
      case 'kakao':
        return authService.getKakaoLoginUrl();
      case 'google':
        return authService.getGoogleLoginUrl();
      case 'naver':
        return authService.getNaverLoginUrl();
      default:
        throw new Error('지원하지 않는 소셜 로그인 제공자입니다');
    }
  }, []);

  // 소셜 로그인 콜백 처리
  const handleSocialCallback = useCallback(async (tokens) => {
    setLoading(true);
    setError(null);
    
    const result = await authService.handleSocialCallback(tokens);
    
    setLoading(false);
    if (!result.success) {
      setError(result.error);
    }
    
    return result;
  }, []);

  // OAuth 콜백 처리
  const handleOAuthCallback = useCallback(async (url) => {
    setLoading(true);
    setError(null);
    
    try {
      // URL에서 토큰 또는 코드 추출
      const urlParams = new URLSearchParams(url.split('?')[1]);
      const code = urlParams.get('code');
      const accessToken = urlParams.get('access_token');
      const error = urlParams.get('error');
      
      if (error) {
        throw new Error('OAuth 인증이 취소되었습니다.');
      }
      
      if (accessToken) {
        // 직접 토큰을 받은 경우
        const result = await authService.handleSocialCallback({ accessToken });
        setLoading(false);
        return result;
      } else if (code) {
        // 인증 코드를 받은 경우 - 백엔드에서 토큰 교환
        const result = await authService.exchangeCodeForToken(code);
        setLoading(false);
        return result;
      } else {
        throw new Error('유효하지 않은 OAuth 응답입니다.');
      }
    } catch (err) {
      setError(err.message);
      setLoading(false);
      return { success: false, error: err.message };
    }
  }, []);

  // 에러 초기화
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    // 상태
    user,
    loading,
    error,
    isAuthenticated: !!user,
    
    // 액션
    loginWithEmail,
    signUp,
    logout,
    getSocialLoginUrl,
    handleSocialCallback,
    handleOAuthCallback,
    clearError,
  };
}