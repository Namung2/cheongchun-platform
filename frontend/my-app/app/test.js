import React, { useState, useEffect } from 'react';
import Config from '../config';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Alert,
  StatusBar
} from 'react-native';
import { useAuth } from '../hooks/useAuth';
import apiService from '../services/ApiService';

export default function TestScreen() {
  const { user, loading } = useAuth();
  const [testResults, setTestResults] = useState([]);
  const [conversationText, setConversationText] = useState('user: 안녕하세요, 요즘 혈압이 높아서 걱정이에요\nassistant: 안녕하세요! 혈압 관리에 대해 걱정하고 계시는군요. 어떤 증상이 있으신가요?\nuser: 가끔 두통이 있고 어지러워요\nassistant: 그런 증상이 있으시면 병원 진료를 받아보시는 것이 좋겠습니다. 평소 운동은 하고 계신가요?');
  const [sessionTitle, setSessionTitle] = useState('혈압 관리 상담');
  const [isLoading, setIsLoading] = useState(false);

  const addTestResult = (message, type = 'info') => {
    const timestamp = new Date().toLocaleTimeString();
    const newResult = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      message,
      type,
      timestamp
    };
    setTestResults(prev => [newResult, ...prev]);
  };

  // 백엔드 헬스체크
  const testBackendHealth = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(Config.API.BASE_URL + '/actuator/health');
      const data = await response.json();
      
      if (data.status === 'UP') {
        addTestResult('✅ 백엔드 정상 작동 중', 'success');
        addTestResult(`DB 상태: ${data.components.db.status}`, 'info');
      } else {
        addTestResult('❌ 백엔드 상태 이상', 'error');
      }
    } catch (error) {
      addTestResult(`❌ 백엔드 연결 실패: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // AI Core 헬스체크
  const testAiCoreHealth = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://10.0.2.2:8001/health');
      const data = await response.json();
      addTestResult('✅ AI Core 정상 작동 중', 'success');
      addTestResult(`AI Core 상태: ${JSON.stringify(data)}`, 'info');
    } catch (error) {
      addTestResult(`❌ AI Core 연결 실패: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // AI 대화 요약 테스트
  const testAiSummary = async () => {
    if (!conversationText.trim()) {
      Alert.alert('오류', '대화 내용을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      const testData = {
        conversation_text: conversationText,
        user_id: parseInt(user?.id) || 1,
        session_title: sessionTitle || '테스트 대화',
        total_messages: conversationText.split('\n').filter(line => line.trim()).length,
        duration_minutes: 5,
        topics: []
      };

      const response = await fetch('http://10.0.2.2:8001/conversation/summary', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(testData)
      });

      if (response.ok) {
        const result = await response.json();
        addTestResult('✅ AI 요약 생성 성공', 'success');
        addTestResult(`📝 요약: ${result.conversation_summary}`, 'info');
        addTestResult(`💡 인사이트: ${result.key_insights.join(', ')}`, 'info');
        addTestResult(`💊 추천: ${result.ai_recommendations.join(', ')}`, 'info');
        addTestResult(`📊 감정: ${result.mood_analysis}, 스트레스: ${result.stress_level}/10`, 'info');
        addTestResult(`🏷️ 주제: ${result.main_topics.join(', ')}`, 'info');
      } else {
        addTestResult('❌ AI 요약 생성 실패', 'error');
      }
    } catch (error) {
      addTestResult(`❌ AI 요약 오류: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // 백엔드 대화 저장 테스트
  const testConversationSave = async () => {
    if (!user) {
      Alert.alert('오류', '로그인이 필요합니다.');
      return;
    }

    setIsLoading(true);
    try {
      // 먼저 AI Core에서 요약 생성
      const summaryResponse = await fetch('http://10.0.2.2:8001/conversation/summary', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          conversation_text: conversationText,
          user_id: parseInt(user.id),
          session_title: sessionTitle,
          total_messages: 4,
          duration_minutes: 5,
          topics: []
        })
      });

      if (summaryResponse.ok) {
        const summaryData = await summaryResponse.json();
        
        // 백엔드에 저장
        const saveData = {
          userId: parseInt(user.id), // Long 타입으로 변환
          sessionTitle: sessionTitle,
          totalMessages: 4,
          durationMinutes: 5,
          conversationText: conversationText,
          mainTopics: summaryData.main_topics || [],
          healthMentions: summaryData.health_mentions || [],
          concernsDiscussed: [],
          moodAnalysis: summaryData.mood_analysis || "neutral",
          stressLevel: summaryData.stress_level || 5,
          conversationSummary: summaryData.conversation_summary || "",
          keyInsights: summaryData.key_insights || [],
          aiRecommendations: summaryData.ai_recommendations || []
        };

        const response = await apiService.saveConversation(saveData);
        addTestResult('✅ 대화 저장 성공', 'success');
        addTestResult(`저장된 대화 ID: ${JSON.stringify(response)}`, 'info');
      } else {
        addTestResult('❌ 요약 생성 실패로 저장 불가', 'error');
      }
    } catch (error) {
      addTestResult(`❌ 대화 저장 오류: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // 대화 목록 조회 테스트
  const testConversationList = async () => {
    if (!user) {
      Alert.alert('오류', '로그인이 필요합니다.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await apiService.request('/ai/conversations?page=0&size=5');
      addTestResult('✅ 대화 목록 조회 성공', 'success');
      addTestResult(`조회된 대화 수: ${response.length}개`, 'info');
      
      response.forEach((conv, index) => {
        addTestResult(`${index + 1}. ${conv.title} (${conv.totalMessages}메시지)`, 'info');
      });
    } catch (error) {
      addTestResult(`❌ 대화 목록 오류: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // 건강 관련 대화 조회 테스트
  const testHealthConversations = async () => {
    if (!user) {
      Alert.alert('오류', '로그인이 필요합니다.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await apiService.request('/ai/conversations/health');
      addTestResult('✅ 건강 대화 조회 성공', 'success');
      addTestResult(`건강 관련 대화 수: ${response.length}개`, 'info');
    } catch (error) {
      addTestResult(`❌ 건강 대화 조회 오류: ${error.message}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const clearResults = () => {
    setTestResults([]);
  };

  useEffect(() => {
    addTestResult('🤖 AI 채팅 시스템 테스트 시작', 'info');
    
    if (loading) {
      addTestResult('현재 사용자: 로딩 중...', 'info');
    } else {
      addTestResult(`현재 사용자: ${user ? user.name || user.email : '비로그인'}`, 'info');
      if (user) {
        addTestResult(`사용자 ID: ${user.id}`, 'info');
      }
    }
  }, [user, loading]);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#2C3E50" />
      
      <View style={styles.header}>
        <Text style={styles.headerTitle}>🤖 AI 시스템 테스트</Text>
        <Text style={styles.headerSubtitle}>비정규화 DB 및 GPT 요약 테스트</Text>
      </View>

      <ScrollView style={styles.content}>
        
        {/* 시스템 상태 테스트 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>📡 시스템 상태</Text>
          <View style={styles.buttonRow}>
            <TouchableOpacity 
              style={[styles.button, styles.primaryButton]} 
              onPress={testBackendHealth}
              disabled={isLoading}
            >
              <Text style={styles.buttonText}>백엔드 확인</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={[styles.button, styles.primaryButton]} 
              onPress={testAiCoreHealth}
              disabled={isLoading}
            >
              <Text style={styles.buttonText}>AI Core 확인</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 대화 입력 영역 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>💬 대화 입력</Text>
          <TextInput
            style={styles.titleInput}
            value={sessionTitle}
            onChangeText={setSessionTitle}
            placeholder="세션 제목"
            placeholderTextColor="#999"
          />
          <TextInput
            style={styles.textArea}
            value={conversationText}
            onChangeText={setConversationText}
            placeholder="대화 내용을 입력하세요..."
            placeholderTextColor="#999"
            multiline
            numberOfLines={8}
          />
        </View>

        {/* AI 기능 테스트 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🧠 AI 기능 테스트</Text>
          <TouchableOpacity 
            style={[styles.button, styles.aiButton]} 
            onPress={testAiSummary}
            disabled={isLoading || !conversationText.trim()}
          >
            <Text style={styles.buttonText}>AI 요약 생성</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.button, styles.aiButton]} 
            onPress={testConversationSave}
            disabled={isLoading || !user || !conversationText.trim()}
          >
            <Text style={styles.buttonText}>대화 저장 (요약만)</Text>
          </TouchableOpacity>
        </View>

        {/* 데이터 조회 테스트 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>📊 데이터 조회</Text>
          <View style={styles.buttonRow}>
            <TouchableOpacity 
              style={[styles.button, styles.dataButton]} 
              onPress={testConversationList}
              disabled={isLoading || !user}
            >
              <Text style={styles.buttonText}>대화 목록</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={[styles.button, styles.dataButton]} 
              onPress={testHealthConversations}
              disabled={isLoading || !user}
            >
              <Text style={styles.buttonText}>건강 대화</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 결과 표시 */}
        <View style={styles.section}>
          <View style={styles.resultHeader}>
            <Text style={styles.sectionTitle}>📋 테스트 결과</Text>
            <TouchableOpacity onPress={clearResults} style={styles.clearButton}>
              <Text style={styles.clearButtonText}>지우기</Text>
            </TouchableOpacity>
          </View>
          
          <ScrollView style={styles.results} nestedScrollEnabled>
            {testResults.map(result => (
              <View key={result.id} style={[
                styles.resultItem,
                styles[`${result.type}Result`]
              ]}>
                <Text style={styles.resultTime}>{result.timestamp}</Text>
                <Text style={styles.resultText}>{result.message}</Text>
              </View>
            ))}
            {testResults.length === 0 && (
              <Text style={styles.noResults}>테스트 결과가 여기에 표시됩니다...</Text>
            )}
          </ScrollView>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#2C3E50',
    paddingVertical: 20,
    paddingHorizontal: 25,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  headerSubtitle: {
    fontSize: 16,
    color: '#E3F2FD',
    marginTop: 5,
  },
  content: {
    flex: 1,
    padding: 15,
  },
  section: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2C3E50',
    marginBottom: 15,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  button: {
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    marginVertical: 5,
  },
  primaryButton: {
    backgroundColor: '#3498DB',
    flex: 0.48,
  },
  aiButton: {
    backgroundColor: '#E74C3C',
  },
  dataButton: {
    backgroundColor: '#27AE60',
    flex: 0.48,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  titleInput: {
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    marginBottom: 10,
    backgroundColor: '#F8F9FA',
  },
  textArea: {
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#F8F9FA',
    textAlignVertical: 'top',
    minHeight: 120,
  },
  resultHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  clearButton: {
    backgroundColor: '#95A5A6',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 6,
  },
  clearButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
  },
  results: {
    maxHeight: 300,
    backgroundColor: '#F8F9FA',
    borderRadius: 8,
    padding: 10,
  },
  resultItem: {
    padding: 10,
    marginVertical: 2,
    borderRadius: 6,
  },
  successResult: {
    backgroundColor: '#D4EDDA',
  },
  errorResult: {
    backgroundColor: '#F8D7DA',
  },
  infoResult: {
    backgroundColor: '#E3F2FD',
  },
  resultTime: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
  },
  resultText: {
    fontSize: 14,
    color: '#2C3E50',
    fontWeight: '500',
  },
  noResults: {
    textAlign: 'center',
    color: '#999',
    fontStyle: 'italic',
    padding: 20,
  },
});