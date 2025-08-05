import { useEffect } from 'react';
import { View, StyleSheet, Image } from 'react-native';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import whiteIcon from '../assets/images/흰글씨표지.png'; // ✅ 올바른 이미지 import

export default function Splash() {
  const router = useRouter();
  
  useEffect(() => {
    const checkAuthStatus = async () => {
      try {
        const token = await AsyncStorage.getItem('accessToken');
        
        setTimeout(() => {
          if (token) {
            // 토큰이 있으면 메인 화면으로
            router.replace('/main');
          } else {
            // 토큰이 없으면 로그인 화면으로
            router.replace('/blank');
          }
        }, 1000);
      } catch (error) {
        console.error('인증 상태 확인 오류:', error);
        setTimeout(() => router.replace('/blank'), 1000);
      }
    };
    
    checkAuthStatus();
  }, []);

  return (
    <View style={[styles.container, { backgroundColor: '#00C853' }]}>
      <Image source={whiteIcon} style={  styles.logoImage} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  logoImage: {
    width: 330,
    height: 330,
    resizeMode: 'contain',
    marginBottom: 200,
  }
});