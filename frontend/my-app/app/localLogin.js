// app/login.js
import { useState } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet } from 'react-native';

export default function localLogin() {
  const [email, setEmail] = useState('');
  return (
    <View style={styles.container}>
      <Text style={styles.title}>이메일 로그인</Text>
      <TextInput
        style={styles.input}
        placeholder="이메일 주소"
        value={email}
        onChangeText={setEmail}
      />
      <TouchableOpacity style={styles.confirmBtn}>
        <Text style={styles.confirmText}>완료</Text>
      </TouchableOpacity>
      <View style={styles.row}>
        <Text>계정이 없으신가요? </Text>
        <Text style={styles.link}>회원 가입</Text>
      </View>
      {/* ...이메일/비밀번호 찾기, SNS 로그인 버튼 등 추가 가능 */}
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
  row: { flexDirection:'row', justifyContent:'center', marginTop:8 },
  link: { color:'#00C853', fontWeight:'500' }
});
