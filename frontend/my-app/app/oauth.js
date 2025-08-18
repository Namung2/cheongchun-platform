import { useEffect, useState } from 'react';
import { View, StyleSheet, Alert, ActivityIndicator, Platform } from 'react-native';
import { WebView } from 'react-native-webview';
import { useRouter, useLocalSearchParams } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import authService from '../services/AuthService';
import Config from '../config';

export default function OAuth() {
  const router = useRouter();
  const { provider } = useLocalSearchParams();
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  const getOAuthUrl = () => {
    const baseUrl = Config.API.BASE_URL + '/oauth2/authorization';
    return `${baseUrl}/${provider}`;
  };

  // 웹 환경에서는 바로 리다이렉트
  useEffect(() => {
    if (Platform.OS === 'web') {
      const oauthUrl = getOAuthUrl();
      window.location.href = oauthUrl;
      return;
    }
  }, [provider]);

  const handleNavigationStateChange = async (navState) => {
    const { url } = navState;
    
    // OAuth 에러 URL 감지
    if (url.includes('/auth/oauth-error')) {
      if (isProcessing) {
        return;
      }
      
      setIsProcessing(true);
      setLoading(true);
      
      try {
        const urlParams = new URLSearchParams(url.split('?')[1]);
        const errorCode = urlParams.get('code');
        const errorMessage = urlParams.get('message');
        
        Alert.alert('로그인 실패', errorMessage || '소셜 로그인에 실패했습니다.', [
          { text: '확인', onPress: () => router.replace('/login') }
        ]);
      } catch (error) {
        Alert.alert('오류', '로그인 처리 중 오류가 발생했습니다.', [
          { text: '확인', onPress: () => router.replace('/login') }
        ]);
      } finally {
        setLoading(false);
        setIsProcessing(false);
      }
      return;
    }
    
    // OAuth 성공 URL 감지 - 백엔드에서 리다이렉트하는 성공 페이지
    if (url.includes('/auth/oauth-success')) {
      // 이미 처리 중이면 중복 실행 방지
      if (isProcessing) {
        return;
      }
      
      setIsProcessing(true);
      setLoading(true);
      
      try {
        // URL에서 토큰과 사용자 정보 추출
        const urlParams = new URLSearchParams(url.split('?')[1]);
        const token = urlParams.get('token');
        const userId = urlParams.get('userId');
        const email = urlParams.get('email');
        const name = urlParams.get('name');
        const provider = urlParams.get('provider');
        
        if (token && userId) {
          // AuthService를 통해 OAuth 성공 처리
          
          const userInfo = { id: userId, email, name, provider };
          const result = await authService.handleOAuthSuccess(token, userInfo);
          
          if (result.success) {
            
            // 팝업 없이 바로 index.js로 이동하여 useAuth가 상태를 업데이트할 시간을 줌
            router.replace('/');
          } else {
            throw new Error(result.error || 'OAuth 처리 실패');
          }
        } else {
          Alert.alert('로그인 실패', '토큰 정보를 찾을 수 없습니다.', [
            { text: '확인', onPress: () => router.replace('/login') }
          ]);
        }
      } catch (error) {
        console.error('OAuth 처리 오류:', error);
        Alert.alert('오류', '로그인 처리 중 오류가 발생했습니다.', [
          { text: '확인', onPress: () => router.replace('/login') }
        ]);
      } finally {
        setLoading(false);
        setIsProcessing(false);
      }
    }
  };

  const handleError = (error) => {
    console.error('WebView Error:', error);
    Alert.alert('오류', '페이지를 불러올 수 없습니다.', [
      { text: '확인', onPress: () => router.back() }
    ]);
  };

  return (
    <View style={styles.container}>
      {loading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#00C853" />
        </View>
      )}
      
      <WebView
        source={{ uri: getOAuthUrl() }}
        onNavigationStateChange={handleNavigationStateChange}
        onError={handleError}
        onLoadStart={() => setLoading(true)}
        onLoadEnd={() => setLoading(false)}
        style={styles.webview}
        javaScriptEnabled={true}
        domStorageEnabled={true}
        startInLoadingState={true}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  webview: {
    flex: 1,
  },
  loadingContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    zIndex: 1,
  },
});