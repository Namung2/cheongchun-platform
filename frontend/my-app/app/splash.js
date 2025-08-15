import { useEffect } from 'react';
import { View, StyleSheet, Image } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../hooks/useAuth';
import whiteIcon from '../assets/images/흰글씨표지.png'; // ✅ 올바른 이미지 import

export default function Splash() {
  const router = useRouter();
  const { isAuthenticated, loading } = useAuth();
  
  useEffect(() => {
    const navigateAfterDelay = () => {
      setTimeout(() => {
        if (isAuthenticated) {
          router.replace('/main');
        } else {
          router.replace('/blank');
        }
      }, 1000);
    };

    // 로딩이 완료된 후에만 네비게이션
    if (!loading) {
      navigateAfterDelay();
    }
  }, [loading, isAuthenticated]);

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