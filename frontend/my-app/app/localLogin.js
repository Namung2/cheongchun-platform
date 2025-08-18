// app/localLogin.js
import { useState } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../hooks/useAuth';

export default function LocalLogin() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const router = useRouter();
  const { loginWithEmail, loading, error } = useAuth();

  const handleLogin = async () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('알림', '이메일과 비밀번호를 모두 입력해주세요.');
      return;
    }

    const result = await loginWithEmail(email.trim(), password);
    
    if (result.success) {
      Alert.alert('성공', '로그인되었습니다!', [
        { text: '확인', onPress: () => router.replace('/main') }
      ]);
    } else {
      Alert.alert('로그인 실패', result.error || '로그인에 실패했습니다.');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>이메일 로그인</Text>
      
      <TextInput
        style={styles.input}
        placeholder="이메일 주소"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        autoComplete="email"
      />
      
      <TextInput
        style={styles.input}
        placeholder="비밀번호"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="password"
      />
      
      {error && (
        <Text style={styles.errorText}>{error}</Text>
      )}
      
      <TouchableOpacity 
        style={[styles.confirmBtn, loading && styles.disabledBtn]} 
        onPress={handleLogin}
        disabled={loading}
      >
        <Text style={styles.confirmText}>
          {loading ? '로그인 중...' : '로그인'}
        </Text>
      </TouchableOpacity>
      
      {/* TODO: signup 페이지 구현 후 활성화
      <View style={styles.row}>
        <Text>계정이 없으신가요? </Text>
        <TouchableOpacity onPress={() => router.push('/signup')}>
          <Text style={styles.link}>회원 가입</Text>
        </TouchableOpacity>
      </View>
      */}
      
      <TouchableOpacity 
        style={styles.backBtn} 
        onPress={() => router.back()}
      >
        <Text style={styles.backText}>뒤로 가기</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex:1, padding:20, paddingTop:60, backgroundColor:'#fff' },
  title: { fontSize:20, fontWeight:'bold', marginBottom:24, color:'#00C853', textAlign:'center' },
  input: {
    borderWidth:1, borderColor:'#ccc', borderRadius:8, paddingHorizontal:12, paddingVertical:10, marginBottom:20
  },
  confirmBtn: {
    backgroundColor:'#00C853', paddingVertical:14, borderRadius:8, alignItems:'center', marginBottom:20
  },
  confirmText: { color:'#fff', fontSize:16, fontWeight:'600' },
  disabledBtn: { backgroundColor:'#ccc' },
  errorText: { color:'#f44336', fontSize:14, marginBottom:10, textAlign:'center' },
  row: { flexDirection:'row', justifyContent:'center', marginTop:8 },
  link: { color:'#00C853', fontWeight:'500' },
  backBtn: { marginTop:20, alignItems:'center' },
  backText: { color:'#666', fontSize:14 }
});
