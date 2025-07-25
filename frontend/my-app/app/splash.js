import { useEffect } from 'react';
import { View, StyleSheet, Image } from 'react-native';
import { useRouter } from 'expo-router';
import whiteIcon from '../assets/images/흰글씨표지.png'; // ✅ 올바른 이미지 import

export default function Splash() {
  const router = useRouter();
  useEffect(() => {
    const t = setTimeout(() => router.replace('/blank'), 1000);
    return () => clearTimeout(t);
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
