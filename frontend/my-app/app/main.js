// app/main.js
import { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../hooks/useAuth';
import apiService from '../services/ApiService';

export default function Main() {
  const router = useRouter();
  const { user, logout, isAuthenticated } = useAuth();
  const [serverStatus, setServerStatus] = useState(null);
  const [meetings, setMeetings] = useState([]);
  const [loading, setLoading] = useState(true);

  // 인증되지 않은 사용자는 로그인 화면으로 리다이렉트
  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/login');
    }
  }, [isAuthenticated]);

  // 서버 상태 및 데이터 로드
  useEffect(() => {
    const loadData = async () => {
      try {
        // 서버 상태 확인
        const health = await apiService.checkHealth();
        setServerStatus(health.status === 'UP' ? 'online' : 'offline');

        // 베스트 모임 목록 조회 (백엔드에 해당 API가 있다면)
        try {
          const meetingsResponse = await apiService.getTodayBestMeetings();
          if (meetingsResponse.success) {
            setMeetings(meetingsResponse.data || []);
          }
        } catch (error) {
          console.log('모임 데이터 로드 실패:', error.message);
          setMeetings([]);
        }
      } catch (error) {
        console.error('데이터 로드 실패:', error);
        setServerStatus('offline');
      } finally {
        setLoading(false);
      }
    };

    if (isAuthenticated) {
      loadData();
    }
  }, [isAuthenticated]);

  const handleLogout = async () => {
    Alert.alert(
      '로그아웃',
      '정말 로그아웃 하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '로그아웃',
          onPress: async () => {
            await logout();
            router.replace('/login');
          }
        }
      ]
    );
  };

  const testApiConnection = async () => {
    try {
      const health = await apiService.checkHealth();
      Alert.alert(
        '서버 연결 테스트',
        `서버 상태: ${health.status}\n데이터베이스: ${health.components?.db?.status || 'Unknown'}\nRedis: ${health.components?.redis?.status || 'Unknown'}`
      );
    } catch (error) {
      Alert.alert('연결 실패', `서버에 연결할 수 없습니다: ${error.message}`);
    }
  };

  if (!isAuthenticated) {
    return null; // 로딩 중이거나 리다이렉트 중
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.welcomeText}>
          안녕하세요, {user?.name || user?.email || '사용자'}님! 👋
        </Text>
        <View style={styles.statusContainer}>
          <Text style={styles.statusLabel}>서버 상태: </Text>
          <Text style={[
            styles.statusText, 
            { color: serverStatus === 'online' ? '#4CAF50' : '#f44336' }
          ]}>
            {serverStatus === 'online' ? '🟢 온라인' : '🔴 오프라인'}
          </Text>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🚀 백엔드 연동 테스트</Text>
        <TouchableOpacity style={styles.testBtn} onPress={testApiConnection}>
          <Text style={styles.testBtnText}>API 연결 테스트</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>👤 사용자 정보</Text>
        <View style={styles.userInfo}>
          <Text style={styles.infoText}>이메일: {user?.email || 'N/A'}</Text>
          <Text style={styles.infoText}>이름: {user?.name || 'N/A'}</Text>
          <Text style={styles.infoText}>
            가입일: {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
          </Text>
        </View>
      </View>

      {meetings.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🎉 오늘의 베스트 모임</Text>
          {meetings.map((meeting, index) => (
            <View key={index} style={styles.meetingCard}>
              <Text style={styles.meetingTitle}>{meeting.title}</Text>
              <Text style={styles.meetingDesc}>{meeting.description}</Text>
            </View>
          ))}
        </View>
      )}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⚙️ 설정</Text>
        <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
          <Text style={styles.logoutBtnText}>로그아웃</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          청춘 앱 v1.0.0 {'\n'}
          백엔드 연동 테스트 완료! 🎯
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#00C853',
    padding: 20,
    paddingTop: 60,
  },
  welcomeText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusLabel: {
    color: '#fff',
    fontSize: 14,
  },
  statusText: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  section: {
    backgroundColor: '#fff',
    margin: 10,
    padding: 15,
    borderRadius: 8,
    elevation: 2,
    boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.2)',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  testBtn: {
    backgroundColor: '#2196F3',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 6,
    alignItems: 'center',
  },
  testBtnText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  userInfo: {
    backgroundColor: '#f9f9f9',
    padding: 10,
    borderRadius: 6,
  },
  infoText: {
    fontSize: 14,
    marginBottom: 5,
    color: '#666',
  },
  meetingCard: {
    backgroundColor: '#f0f8ff',
    padding: 10,
    borderRadius: 6,
    marginBottom: 8,
  },
  meetingTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
  },
  meetingDesc: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  logoutBtn: {
    backgroundColor: '#f44336',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 6,
    alignItems: 'center',
  },
  logoutBtnText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  footer: {
    padding: 20,
    alignItems: 'center',
  },
  footerText: {
    fontSize: 12,
    color: '#999',
    textAlign: 'center',
    lineHeight: 18,
  },
});