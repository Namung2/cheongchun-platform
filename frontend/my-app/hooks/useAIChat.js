import { useState, useRef, useCallback, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import AiCoreService from '../services/AiCoreService';
import apiService from '../services/ApiService';

export const useAIChat = (userId) => {
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [currentResponse, setCurrentResponse] = useState('');
  const wsRef = useRef(null);
  const sessionRef = useRef({
    startTime: null,
    messageCount: 0,
    conversationData: []
  });

  // WebSocket 연결 (개선된 버전)
  const connectWebSocket = useCallback(async () => {
    try {
      if (!userId) {
        console.error('userId not available');
        return;
      }

      // WebSocket 연결 URL 생성 (Android 에뮬레이터 대응)
      const baseUrl = __DEV__ && Platform.OS === 'android' ? '10.0.2.2:8001' : 'localhost:8001';
      const wsUrl = `ws://${baseUrl}/ws/chat/${userId}`;
      
      wsRef.current = new WebSocket(wsUrl);
      
      wsRef.current.onopen = () => {
        setIsConnected(true);
        // 세션 시작
        sessionRef.current.startTime = new Date();
        sessionRef.current.messageCount = 0;
        sessionRef.current.conversationData = [];
      };

      wsRef.current.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          
          switch (data.type) {
            case 'chunk':
              // 실시간 스트리밍 청크
              setCurrentResponse(prev => prev + data.content);
              setIsTyping(true);
              break;
              
            case 'complete':
              // 완성된 응답
              const aiMessage = {
                _id: Math.random().toString(36).substring(7),
                text: data.content,
                createdAt: new Date(data.timestamp),
                user: {
                  _id: 'ai-assistant',
                  name: '시니어 도우미',
                  avatar: '🤖'
                }
              };
              
              // 대화 데이터에 AI 응답 추가
              sessionRef.current.conversationData.push({
                role: 'assistant',
                content: data.content,
                timestamp: new Date(data.timestamp)
              });
              sessionRef.current.messageCount++;
              
              setMessages(previousMessages => 
                [aiMessage, ...previousMessages]
              );
              setIsTyping(false);
              setCurrentResponse('');
              
              // 실시간 저장 제거 - 세션 종료시에만 저장
              break;
              
            case 'error':
              setIsTyping(false);
              setCurrentResponse('');
              break;
          }
        } catch (err) {
          // 메시지 파싱 오류 무시
        }
      };

      wsRef.current.onerror = (error) => {
        setIsConnected(false);
        setIsTyping(false);
      };

      wsRef.current.onclose = (event) => {
        setIsConnected(false);
        setIsTyping(false);
        
        // 세션 종료시 대화 요약 저장
        if (sessionRef.current.messageCount >= 2) {
          saveConversationSummary();
        }
        
        // 비정상 종료시 재연결 시도
        if (event.code !== 1000) {
          setTimeout(() => {
            connectWebSocket();
          }, 3000);
        }
      };

    } catch (error) {
      setIsConnected(false);
    }
  }, [userId]);

  // AI 대화 요약 생성 및 저장
  const saveConversationSummary = useCallback(async () => {
    if (!userId || sessionRef.current.conversationData.length === 0) return;

    try {
      const startTime = sessionRef.current.startTime;
      const endTime = new Date();
      const duration = Math.floor((endTime - startTime) / 1000 / 60); // 분 단위

      // 대화 내용을 텍스트로 변환
      const conversationText = sessionRef.current.conversationData
        .map(msg => `${msg.role}: ${msg.content}`)
        .join('\n');

      const sessionTitle = generateSessionTitle(sessionRef.current.conversationData);
      const basicTopics = extractTopics(conversationText);

      // AI Core에 요약 생성 요청
      const aiCoreResponse = await fetch('http://localhost:8001/conversation/summary', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          conversation_text: conversationText,
          user_id: parseInt(userId),
          session_title: sessionTitle,
          total_messages: sessionRef.current.messageCount,
          duration_minutes: duration,
          topics: basicTopics
        })
      });

      if (!aiCoreResponse.ok) {
        throw new Error('AI 요약 생성 실패');
      }

      const summaryData = await aiCoreResponse.json();

      // 백엔드에 저장할 데이터 구성 (원본 메시지 제외)
      const saveRequest = {
        userId: parseInt(userId),
        sessionTitle: sessionTitle,
        totalMessages: sessionRef.current.messageCount,
        durationMinutes: duration,
        conversationText: conversationText, // AI 분석용
        mainTopics: summaryData.main_topics,
        healthMentions: summaryData.health_mentions,
        concernsDiscussed: [], // 필요시 추가
        moodAnalysis: summaryData.mood_analysis,
        stressLevel: summaryData.stress_level,
        conversationSummary: summaryData.conversation_summary,
        keyInsights: summaryData.key_insights,
        aiRecommendations: summaryData.ai_recommendations
      };

      // 백엔드에 요약만 저장 (원본 메시지는 저장하지 않음)
      const response = await apiService.saveConversation(saveRequest);
      console.log('대화 요약 저장 완료:', response);

    } catch (error) {
      console.error('대화 요약 저장 실패:', error);
    }
  }, [userId]);

  // 세션 제목 생성
  const generateSessionTitle = useCallback((conversationData) => {
    if (conversationData.length === 0) return '대화';
    
    const firstUserMessage = conversationData.find(msg => msg.role === 'user')?.content || '';
    return firstUserMessage.slice(0, 20) + (firstUserMessage.length > 20 ? '...' : '');
  }, []);

  // 주요 주제 추출 (간단한 키워드 기반)
  const extractTopics = useCallback((text) => {
    const healthKeywords = ['건강', '병원', '약', '운동', '식사', '혈압', '당뇨'];
    const familyKeywords = ['가족', '자식', '손주', '며느리', '사위'];
    const hobbyKeywords = ['취미', '여행', '독서', '운동', '요리', '정원'];
    
    const topics = [];
    
    if (healthKeywords.some(keyword => text.includes(keyword))) {
      topics.push('건강');
    }
    if (familyKeywords.some(keyword => text.includes(keyword))) {
      topics.push('가족');
    }
    if (hobbyKeywords.some(keyword => text.includes(keyword))) {
      topics.push('취미');
    }
    
    return topics.length > 0 ? topics : ['일상'];
  }, []);

  // 건강 관련 키워드 추출
  const extractHealthMentions = useCallback((text) => {
    const healthTerms = ['혈압', '당뇨', '관절', '심장', '약', '병원', '운동', '식단'];
    return healthTerms.filter(term => text.includes(term));
  }, []);

  // WebSocket 연결 해제
  const disconnectWebSocket = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      // 정상 종료시에도 요약 저장
      if (sessionRef.current.messageCount >= 2) {
        saveConversationSummary();
      }
      wsRef.current.close();
    }
  }, [saveConversationSummary]);

  // 메시지 전송 (WebSocket 방식)
  const sendMessage = useCallback((newMessages = []) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      // WebSocket이 연결되지 않았을 때 사용자에게 알림
      const errorMessage = {
        _id: Math.random().toString(36).substring(7),
        text: '연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.',
        createdAt: new Date(),
        user: {
          _id: 'ai-assistant',
          name: '시니어 도우미',
          avatar: '❌'
        }
      };
      
      setMessages(previousMessages => [errorMessage, ...previousMessages]);
      return;
    }

    const message = newMessages[0];
    if (!message) return;

    // 사용자 메시지를 즉시 UI에 추가
    setMessages(previousMessages => [message, ...previousMessages]);

    // 채팅 히스토리 준비 (최근 10개 메시지)
    const chatHistory = messages.slice(0, 10).reverse().map(msg => ({
      role: msg.user._id === 'ai-assistant' ? 'assistant' : 'user',
      content: msg.text
    }));

    // WebSocket으로 메시지 전송
    const payload = {
      message: message.text,
      history: chatHistory,
      timestamp: message.createdAt.toISOString()
    };

    try {
      wsRef.current.send(JSON.stringify(payload));
      setIsTyping(true);
    } catch (error) {
      // WebSocket 전송 실패 무시
    }
  }, [messages]);

  // 컴포넌트 마운트 시 WebSocket 연결
  useEffect(() => {
    if (userId) {
      connectWebSocket();
    }

    return () => {
      disconnectWebSocket();
    };
  }, [userId, connectWebSocket, disconnectWebSocket]);

  // 시니어 친화적 초기 메시지
  useEffect(() => {
    const initialMessage = {
      _id: 'welcome-message',
      text: `안녕하세요! 😊\n\n저는 여러분을 도와드리는 AI 도우미입니다.\n\n• 건강 상담\n• 생활 정보\n• 취미 활동\n• 가족 관계\n\n편안하게 무엇이든 물어보세요!`,
      createdAt: new Date(),
      user: {
        _id: 'ai-assistant',
        name: '시니어 도우미',
        avatar: '🤖'
      }
    };

    setMessages([initialMessage]);
  }, []);

  return {
    messages,
    isConnected,
    isTyping,
    currentResponse,
    sendMessage,
    reconnect: connectWebSocket
  };
};