import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ActivityIndicator,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import apiService from '../../services/ApiService';

export default function VerifyEmailScreen() {
  const router = useRouter();
  const { token } = useLocalSearchParams();
  const [loading, setLoading] = useState(true);
  const [verificationResult, setVerificationResult] = useState(null);

  useEffect(() => {
    verifyEmail();
  }, [token]);

  const verifyEmail = async () => {
    if (!token) {
      setVerificationResult({
        success: false,
        message: '유효하지 않은 인증 링크입니다.',
      });
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const response = await apiService.request(`/auth/verify-email?token=${token}`, {
        method: 'GET',
      });

      console.log('Email verification response:', response);

      if (response.success) {
        setVerificationResult({
          success: true,
          message: '이메일 인증이 완료되었습니다! 이제 로그인할 수 있습니다.',
        });
      } else {
        setVerificationResult({
          success: false,
          message: response.error?.message || '이메일 인증에 실패했습니다.',
        });
      }
    } catch (error) {
      console.error('Email verification error:', error);
      setVerificationResult({
        success: false,
        message: '인증 처리 중 오류가 발생했습니다.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = () => {
    setVerificationResult(null);
    verifyEmail();
  };

  const handleGoToLogin = () => {
    router.replace('/login/login');
  };

  const handleResendEmail = () => {
    Alert.prompt(
      '인증 이메일 재발송',
      '이메일 주소를 입력해주세요.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '재발송',
          onPress: async (email) => {
            if (!email) {
              Alert.alert('알림', '이메일 주소를 입력해주세요.');
              return;
            }
            
            try {
              const response = await apiService.resendVerificationEmail(email);
              if (response.success) {
                Alert.alert('성공', '인증 이메일이 재발송되었습니다. 이메일을 확인해주세요.');
              } else {
                Alert.alert('실패', response.error?.message || '이메일 재발송에 실패했습니다.');
              }
            } catch (error) {
              Alert.alert('오류', '이메일 재발송 중 오류가 발생했습니다.');
            }
          },
        },
      ],
      'plain-text',
      '',
      'email-address'
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>이메일 인증</Text>
      </View>

      <View style={styles.content}>
        {loading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#00C853" />
            <Text style={styles.loadingText}>이메일 인증을 처리하고 있습니다...</Text>
          </View>
        ) : (
          <View style={styles.resultContainer}>
            <View style={styles.iconContainer}>
              <Ionicons
                name={verificationResult?.success ? 'checkmark-circle' : 'close-circle'}
                size={80}
                color={verificationResult?.success ? '#00C853' : '#f44336'}
              />
            </View>

            <Text style={styles.resultTitle}>
              {verificationResult?.success ? '인증 성공!' : '인증 실패'}
            </Text>

            <Text style={styles.resultMessage}>
              {verificationResult?.message}
            </Text>

            <View style={styles.buttonContainer}>
              {verificationResult?.success ? (
                <TouchableOpacity style={styles.primaryButton} onPress={handleGoToLogin}>
                  <Text style={styles.primaryButtonText}>로그인하러 가기</Text>
                </TouchableOpacity>
              ) : (
                <View>
                  <TouchableOpacity style={styles.primaryButton} onPress={handleRetry}>
                    <Text style={styles.primaryButtonText}>다시 시도</Text>
                  </TouchableOpacity>
                  
                  <TouchableOpacity style={styles.secondaryButton} onPress={handleResendEmail}>
                    <Text style={styles.secondaryButtonText}>인증 이메일 재발송</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </View>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  backButton: {
    position: 'absolute',
    left: 16,
    zIndex: 1,
    padding: 5,
  },
  headerTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: '600',
    textAlign: 'center',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  loadingContainer: {
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 20,
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  resultContainer: {
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 20,
  },
  resultTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  resultMessage: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  buttonContainer: {
    width: '100%',
  },
  primaryButton: {
    backgroundColor: '#00C853',
    paddingVertical: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 12,
  },
  primaryButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    paddingVertical: 16,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#00C853',
  },
  secondaryButtonText: {
    color: '#00C853',
    fontSize: 16,
    fontWeight: '500',
  },
});