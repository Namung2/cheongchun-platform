import React, { useState } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  SafeAreaView, 
  Alert,
  TouchableOpacity,
  StatusBar,
  TextInput,
  FlatList,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import { useAIChat } from '../hooks/useAIChat';
import { useAuth } from '../hooks/useAuth';
import { Ionicons } from '@expo/vector-icons';

export default function ChatScreen() {
  const { user } = useAuth();
  const { 
    messages, 
    isConnected, 
    isTyping, 
    currentResponse, 
    sendMessage, 
    reconnect 
  } = useAIChat(user?.id);

  const [inputText, setInputText] = useState('');

  const handleSend = () => {
    if (!inputText.trim()) return;
    
    const message = {
      _id: Math.random().toString(36).substring(7),
      text: inputText,
      createdAt: new Date(),
      user: {
        _id: user?.id || '1',
        name: user?.name || '사용자'
      }
    };
    
    sendMessage([message]);
    setInputText('');
  };

  const renderMessage = ({ item }) => {
    const isUser = item.user._id !== 'ai-assistant';
    
    return (
      <View style={[
        styles.messageContainer,
        isUser ? styles.userMessageContainer : styles.aiMessageContainer
      ]}>
        <View style={[
          styles.messageBubble,
          isUser ? styles.userMessage : styles.aiMessage
        ]}>
          <Text style={[
            styles.messageText,
            isUser ? styles.userText : styles.aiText
          ]}>
            {item.text}
          </Text>
          <Text style={[
            styles.timeText,
            isUser ? styles.userTimeText : styles.aiTimeText
          ]}>
            {item.createdAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </Text>
        </View>
      </View>
    );
  };

  // 연결 상태 확인 및 재연결
  const handleReconnect = () => {
    Alert.alert(
      "연결 재시도",
      "AI 도우미와 다시 연결하시겠습니까?",
      [
        { text: "취소", style: "cancel" },
        { text: "재연결", onPress: reconnect }
      ]
    );
  };

  // 연결 상태 표시
  const renderConnectionStatus = () => {
    return (
      <View style={[
        styles.connectionStatus, 
        { backgroundColor: isConnected ? '#4CAF50' : '#FF5722' }
      ]}>
        <Text style={styles.connectionText}>
          {isConnected ? '🟢 AI 도우미 연결됨' : '🔴 연결 끊어짐'}
        </Text>
        {!isConnected && (
          <TouchableOpacity onPress={handleReconnect} style={styles.reconnectButton}>
            <Text style={styles.reconnectText}>재연결</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  };

  // 타이핑 인디케이터
  const renderTyping = () => {
    if (!isTyping) return null;
    
    return (
      <View style={styles.typingContainer}>
        <Text style={styles.typingText}>
          AI 도우미가 답변을 작성 중입니다...
        </Text>
        {currentResponse && (
          <Text style={styles.currentResponse}>
            {currentResponse}
          </Text>
        )}
      </View>
    );
  };

  if (!user) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.loginRequired}>
          <Text style={styles.loginText}>AI 채팅을 사용하려면 로그인이 필요합니다.</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#2C3E50" />
      
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>시니어 AI 도우미</Text>
        <Text style={styles.headerSubtitle}>언제든지 편하게 물어보세요</Text>
      </View>

      {/* 연결 상태 */}
      {renderConnectionStatus()}

      <KeyboardAvoidingView 
        style={styles.chatContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        {/* 채팅 메시지 */}
        <FlatList
          data={messages}
          renderItem={renderMessage}
          keyExtractor={(item) => item._id}
          style={styles.messagesList}
          contentContainerStyle={styles.messagesContent}
          inverted
          showsVerticalScrollIndicator={false}
        />

        {/* 타이핑 인디케이터 */}
        {renderTyping()}

        {/* 입력 영역 */}
        <View style={styles.inputContainer}>
          <TextInput
            style={styles.textInput}
            value={inputText}
            onChangeText={setInputText}
            placeholder="메시지를 입력하세요..."
            placeholderTextColor="#999"
            multiline
            maxHeight={120}
          />
          <TouchableOpacity 
            style={styles.sendButton}
            onPress={handleSend}
            disabled={!inputText.trim()}
          >
            <Ionicons 
              name="send" 
              size={24} 
              color={inputText.trim() ? "#FFFFFF" : "#999"} 
            />
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FAFBFC',
  },
  header: {
    backgroundColor: '#2C3E50',
    paddingVertical: 20,
    paddingHorizontal: 25,
    borderBottomWidth: 0,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  headerTitle: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#FFFFFF',
    textAlign: 'center',
  },
  headerSubtitle: {
    fontSize: 18,
    color: '#E3F2FD',
    textAlign: 'center',
    marginTop: 8,
  },
  connectionStatus: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 20,
  },
  connectionText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  reconnectButton: {
    backgroundColor: 'rgba(255,255,255,0.3)',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 15,
  },
  reconnectText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  chatContainer: {
    flex: 1,
  },
  messagesList: {
    flex: 1,
    paddingHorizontal: 15,
  },
  messagesContent: {
    paddingVertical: 10,
  },
  messageContainer: {
    marginVertical: 4,
  },
  userMessageContainer: {
    alignItems: 'flex-end',
  },
  aiMessageContainer: {
    alignItems: 'flex-start',
  },
  messageBubble: {
    maxWidth: '85%',
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderRadius: 25,
    marginHorizontal: 5,
  },
  userMessage: {
    backgroundColor: '#4A90E2',
  },
  aiMessage: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  messageText: {
    fontSize: 18,
    lineHeight: 26,
    fontWeight: '500',
  },
  userText: {
    color: '#FFFFFF',
  },
  aiText: {
    color: '#2C3E50',
  },
  timeText: {
    fontSize: 12,
    marginTop: 5,
    opacity: 0.7,
  },
  userTimeText: {
    color: '#E3F2FD',
    textAlign: 'right',
  },
  aiTimeText: {
    color: '#7F8C8D',
  },
  typingContainer: {
    backgroundColor: '#E3F2FD',
    padding: 20,
    margin: 15,
    borderRadius: 25,
    borderWidth: 1,
    borderColor: '#BBDEFB',
  },
  typingText: {
    fontSize: 18,
    color: '#1976D2',
    fontStyle: 'italic',
    fontWeight: '600',
  },
  currentResponse: {
    fontSize: 18,
    color: '#2C3E50',
    marginTop: 10,
    lineHeight: 26,
    fontWeight: '500',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 15,
    paddingVertical: 10,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 25,
    paddingHorizontal: 20,
    paddingVertical: 15,
    fontSize: 18,
    color: '#2C3E50',
    backgroundColor: '#F8F9FA',
    marginRight: 10,
  },
  sendButton: {
    width: 50,
    height: 50,
    backgroundColor: '#4A90E2',
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loginRequired: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 30,
    backgroundColor: '#FAFBFC',
  },
  loginText: {
    fontSize: 22,
    textAlign: 'center',
    color: '#2C3E50',
    fontWeight: '600',
    lineHeight: 32,
  },
});

// 파일 끝에 export 추가하지 않습니다 - 이미 default export가 함수 선언에 있음