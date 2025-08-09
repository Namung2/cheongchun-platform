import { useState, useRef, useCallback, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import AiCoreService from '../services/AiCoreService';

export const useAIChat = (userId) => {
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [currentResponse, setCurrentResponse] = useState('');
  const wsRef = useRef(null);

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
              
              setMessages(previousMessages => 
                [aiMessage, ...previousMessages]
              );
              setIsTyping(false);
              setCurrentResponse('');
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

  // WebSocket 연결 해제
  const disconnectWebSocket = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.close();
    }
  }, []);

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