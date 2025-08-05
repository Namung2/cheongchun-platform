// app/blank.js
import { useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';

export default function Blank() {
  const router = useRouter();
  useEffect(() => {
    const t = setTimeout(() => router.replace('/login'), 500);
    return () => clearTimeout(t);
  }, []);

  return <View style={styles.container} />;
}

const styles = StyleSheet.create({
  container: { flex:1, backgroundColor:'#fff' },
});