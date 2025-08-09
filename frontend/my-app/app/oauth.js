import { useEffect, useState } from 'react';
import { View, StyleSheet, Alert, ActivityIndicator, Platform } from 'react-native';
import { WebView } from 'react-native-webview';
import { useRouter, useLocalSearchParams } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import authService from '../services/AuthService';

export default function OAuth() {
  const router = useRouter();
  const { provider } = useLocalSearchParams();
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  const getOAuthUrl = () => {
    const baseUrl = 'https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/oauth2/authorization';
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
        
        if (token && userId) {
          // AuthService를 통해 OAuth 성공 처리
          console.log('토큰과 사용자 정보 처리 시작:', { token, userId, email, name });
          
          const userInfo = { id: userId, email, name };
          const result = await authService.handleOAuthSuccess(token, userInfo);
          
          if (result.success) {
            console.log('OAuth 인증 상태 업데이트 완료');
            
            Alert.alert(
              '로그인 성공!', 
              '로그인이 완료되었습니다.', 
              [{
                text: '확인', 
                onPress: () => {
                  // WebView를 닫고 메인으로 바로 이동
                  router.replace('/main');
                }
              }],
              { cancelable: false } // 뒤로가기 등으로 닫기 방지
            );
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