// app/login.js
import { useEffect } from 'react';
import { View, StyleSheet, Image, TouchableOpacity, Text } from 'react-native';
import { useRouter } from 'expo-router';
import Animated, { Keyframe } from 'react-native-reanimated';
import greenIcon from '../assets/images/표지.png';
import Config from '../config';

// 아래에서 위로 슬라이드 인
const SlideInFromBottom = new Keyframe({
  from: { transform: [{ translateY: 300 }] },
  to:   { transform: [{ translateY:   0 }] },
});

// 위에서 아래로 슬라이드 아웃
const SlideOutToBottom = new Keyframe({
  from: { transform: [{ translateY:   0 }] },
  to:   { transform: [{ translateY: 300 }] },
});

export default function login() {
  const router = useRouter();

  return (
    <View style={styles.container}>
      <Image source={greenIcon} style={styles.loginImage} />

      <Animated.View
        entering={SlideInFromBottom.duration(500)}
        exiting={SlideOutToBottom.duration(300)}
        style={styles.bottomBox}
      >
        {/* 이메일 로그인 */}
        <TouchableOpacity
          style={styles.button}
          onPress={() => router.push('/localLogin')}
        >
          <Text style={styles.buttonText}>이메일 로그인</Text>
        </TouchableOpacity>

        {/* 카카오 로그인 */}
        <TouchableOpacity
          style={[styles.button, styles.kakaoButton]}
          onPress={() => router.push('/oauth?provider=kakao')}
        >
          <Text style={styles.buttonText}>카카오 로그인</Text>
        </TouchableOpacity>

        {/* Google 계정 로그인 */}
        <TouchableOpacity
          style={[styles.button, styles.googleButton]}
          onPress={() => {
            // 모든 OAuth 방식을 WebView로 통일
            router.push('/oauth?provider=google');
          }}
        >
          <Text style={[styles.buttonText, styles.whiteText]}>
            Google 계정 로그인
          </Text>
        </TouchableOpacity>

        {/* 회원 가입 - TODO: signup 페이지 구현 후 활성화
        <TouchableOpacity
          style={[styles.button, styles.signupButton]}
          onPress={() => router.push('/signup')}
        >
          <Text style={[styles.buttonText, styles.whiteText]}>
            회원 가입
          </Text>
        </TouchableOpacity>
        */}

      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  loginImage: {
    width: 350,
    height: 350,
    resizeMode: 'contain',
    marginBottom: 200,
  },
  bottomBox: {
    position: 'absolute',
    bottom: 0,
    width: '100%',
    backgroundColor: '#00C853',
    paddingVertical: 24,
    paddingHorizontal: 20,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  button: {
    backgroundColor: '#fff',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 12,
  },
  buttonText: {
    color: '#000',
    fontSize: 16,
    fontWeight: '500',
  },
  whiteText: {
    color: '#fff',
  },
  kakaoButton: {
    backgroundColor: '#FEE500',
  },
  googleButton: {
    backgroundColor: '#4285F4',
  },
  signupButton: {
    backgroundColor: '#34495E',
  },
});