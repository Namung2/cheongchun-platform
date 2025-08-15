// app/_layout.js
import { Stack } from 'expo-router';
import { useEffect, useState } from 'react';
import * as Linking from 'expo-linking';
import { useRouter } from 'expo-router';
import authService from '../services/AuthService';

export default function RootLayout() {
  const [isReady, setIsReady] = useState(false);
  const router = useRouter();

  useEffect(() => {
    // 딥링크 처리
    const handleDeepLink = async (url) => {
      console.log('Deep link received:', url);
      
      if (url && url.includes('auth-success')) {
        try {
          const urlParts = url.split('?');
          if (urlParts.length < 2) {
            console.error('Invalid deep link format:', url);
            router.replace('/login');
            return;
          }
          
          const urlParams = new URLSearchParams(urlParts[1]);
          const token = urlParams.get('token');
          const userId = urlParams.get('userId');
          const email = urlParams.get('email');
          const name = urlParams.get('name');
          
          console.log('Extracted params:', { token: !!token, userId, email, name });
          
          if (token && userId) {
            const userInfo = { id: userId, email, name };
            const result = await authService.handleOAuthSuccess(token, userInfo);
            
            if (result.success) {
              console.log('OAuth success, navigating to home');
              router.replace('/');
            } else {
              console.error('OAuth handling failed:', result.error);
              router.replace('/login');
            }
          } else {
            console.error('Missing required parameters:', { token: !!token, userId });
            router.replace('/login');
          }
        } catch (error) {
          console.error('Deep link processing error:', error);
          router.replace('/login');
        }
      } else if (url && url.includes('auth-error')) {
        console.error('OAuth error received via deep link');
        router.replace('/login');
      }
    };

    // 딥링크 리스너 등록
    const subscription = Linking.addEventListener('url', ({ url }) => handleDeepLink(url));

    // 앱 실행 시 딥링크 확인
    Linking.getInitialURL().then(url => {
      if (url) {
        handleDeepLink(url);
      }
    });

    setIsReady(true);

    // 클리너 함수
    return () => {
      subscription?.remove();
    };
  }, []);

  if (!isReady) {
    return null;
  }

  return (
    <Stack
      screenOptions={{ headerShown: false }}
    />
  );
}