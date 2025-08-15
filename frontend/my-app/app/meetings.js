import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Alert,
  StatusBar,
  Modal,
  RefreshControl
} from 'react-native';
import { useAuth } from '../hooks/useAuth';
import apiService from '../services/ApiService';

export default function MeetingsScreen() {
  const { user, loading } = useAuth();
  const [meetings, setMeetings] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedMeeting, setSelectedMeeting] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  
  // 모임 생성 폼 데이터
  const [newMeeting, setNewMeeting] = useState({
    title: '',
    description: '',
    location: '',
    maxParticipants: '',
    meetingDate: '',
    category: '문화/취미'
  });

  // 카테고리 옵션
  const categories = [
    '문화/취미', '운동/건강', '여행', '교육/학습', 
    '봉사활동', '모임/만남', '기타'
  ];

  useEffect(() => {
    if (user) {
      loadMeetings();
    }
  }, [user]);

  // 모임 목록 로드
  const loadMeetings = async () => {
    if (isLoading) return;
    
    setIsLoading(true);
    try {
      const response = await apiService.getMeetings();
      if (response.success) {
        setMeetings(response.data.content || []);
      } else {
        Alert.alert('오류', '모임 목록을 불러오는데 실패했습니다.');
      }
    } catch (error) {
      Alert.alert('오류', `모임 목록 조회 실패: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  // 새로고침
  const onRefresh = async () => {
    setRefreshing(true);
    await loadMeetings();
    setRefreshing(false);
  };

  // 모임 생성
  const createMeeting = async () => {
    if (!newMeeting.title || !newMeeting.location || !newMeeting.maxParticipants) {
      Alert.alert('알림', '필수 항목을 모두 입력해주세요.');
      return;
    }

    try {
      const meetingData = {
        title: newMeeting.title,
        description: newMeeting.description,
        location: newMeeting.location,
        maxParticipants: parseInt(newMeeting.maxParticipants),
        meetingDate: newMeeting.meetingDate || new Date().toISOString(),
        category: newMeeting.category
      };

      const response = await apiService.request('/meetings', {
        method: 'POST',
        body: JSON.stringify(meetingData)
      });

      if (response.success) {
        Alert.alert('성공', '모임이 생성되었습니다!');
        setShowCreateModal(false);
        setNewMeeting({
          title: '',
          description: '',
          location: '',
          maxParticipants: '',
          meetingDate: '',
          category: '문화/취미'
        });
        loadMeetings();
      } else {
        Alert.alert('오류', response.error || '모임 생성에 실패했습니다.');
      }
    } catch (error) {
      Alert.alert('오류', `모임 생성 실패: ${error.message}`);
    }
  };

  // 모임 참여
  const joinMeeting = async (meetingId) => {
    try {
      const response = await apiService.joinMeeting(meetingId);
      if (response.success) {
        Alert.alert('성공', '모임에 참여했습니다!');
        loadMeetings();
        setShowDetailModal(false);
      } else {
        Alert.alert('오류', response.error || '모임 참여에 실패했습니다.');
      }
    } catch (error) {
      Alert.alert('오류', `모임 참여 실패: ${error.message}`);
    }
  };

  // 모임 상세보기
  const showMeetingDetail = async (meeting) => {
    try {
      const response = await apiService.getMeetingDetail(meeting.id);
      if (response.success) {
        setSelectedMeeting(response.data);
        setShowDetailModal(true);
      } else {
        Alert.alert('오류', '모임 상세 정보를 불러오는데 실패했습니다.');
      }
    } catch (error) {
      Alert.alert('오류', `모임 상세 조회 실패: ${error.message}`);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </SafeAreaView>
    );
  }

  if (!user) {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.errorText}>로그인이 필요합니다.</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#4A90E2" />
      
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>모임 관리</Text>
        <TouchableOpacity 
          style={styles.createButton}
          onPress={() => setShowCreateModal(true)}
        >
          <Text style={styles.createButtonText}>+ 모임 만들기</Text>
        </TouchableOpacity>
      </View>

      {/* 모임 목록 */}
      <ScrollView 
        style={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        {meetings.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>등록된 모임이 없습니다.</Text>
            <TouchableOpacity 
              style={styles.primaryButton}
              onPress={() => setShowCreateModal(true)}
            >
              <Text style={styles.primaryButtonText}>첫 모임 만들기</Text>
            </TouchableOpacity>
          </View>
        ) : (
          meetings.map((meeting) => (
            <TouchableOpacity 
              key={meeting.id}
              style={styles.meetingCard}
              onPress={() => showMeetingDetail(meeting)}
            >
              <View style={styles.meetingHeader}>
                <Text style={styles.meetingTitle}>{meeting.title}</Text>
                <Text style={styles.meetingCategory}>{meeting.category}</Text>
              </View>
              <Text style={styles.meetingDescription} numberOfLines={2}>
                {meeting.description}
              </Text>
              <View style={styles.meetingInfo}>
                <Text style={styles.meetingLocation}>📍 {meeting.location}</Text>
                <Text style={styles.meetingParticipants}>
                  👥 {meeting.currentParticipants || 0}/{meeting.maxParticipants}
                </Text>
              </View>
              <Text style={styles.meetingDate}>
                🗓️ {new Date(meeting.meetingDate).toLocaleDateString()}
              </Text>
            </TouchableOpacity>
          ))
        )}
      </ScrollView>

      {/* 모임 생성 모달 */}
      <Modal
        visible={showCreateModal}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>새 모임 만들기</Text>
              <TouchableOpacity onPress={() => setShowCreateModal(false)}>
                <Text style={styles.closeButton}>✕</Text>
              </TouchableOpacity>
            </View>
            
            <ScrollView style={styles.modalContent}>
              <Text style={styles.inputLabel}>모임 제목 *</Text>
              <TextInput
                style={styles.textInput}
                value={newMeeting.title}
                onChangeText={(text) => setNewMeeting({...newMeeting, title: text})}
                placeholder="모임 제목을 입력하세요"
              />

              <Text style={styles.inputLabel}>설명</Text>
              <TextInput
                style={[styles.textInput, styles.multilineInput]}
                value={newMeeting.description}
                onChangeText={(text) => setNewMeeting({...newMeeting, description: text})}
                placeholder="모임에 대한 설명을 입력하세요"
                multiline
                numberOfLines={3}
              />

              <Text style={styles.inputLabel}>장소 *</Text>
              <TextInput
                style={styles.textInput}
                value={newMeeting.location}
                onChangeText={(text) => setNewMeeting({...newMeeting, location: text})}
                placeholder="모임 장소를 입력하세요"
              />

              <Text style={styles.inputLabel}>최대 참여자 수 *</Text>
              <TextInput
                style={styles.textInput}
                value={newMeeting.maxParticipants}
                onChangeText={(text) => setNewMeeting({...newMeeting, maxParticipants: text})}
                placeholder="최대 참여자 수"
                keyboardType="numeric"
              />

              <Text style={styles.inputLabel}>카테고리</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryContainer}>
                {categories.map((category) => (
                  <TouchableOpacity
                    key={category}
                    style={[
                      styles.categoryChip,
                      newMeeting.category === category && styles.categoryChipActive
                    ]}
                    onPress={() => setNewMeeting({...newMeeting, category})}
                  >
                    <Text style={[
                      styles.categoryChipText,
                      newMeeting.category === category && styles.categoryChipTextActive
                    ]}>
                      {category}
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </ScrollView>

            <View style={styles.modalButtons}>
              <TouchableOpacity 
                style={styles.cancelButton}
                onPress={() => setShowCreateModal(false)}
              >
                <Text style={styles.cancelButtonText}>취소</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={styles.primaryButton}
                onPress={createMeeting}
              >
                <Text style={styles.primaryButtonText}>생성</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* 모임 상세 모달 */}
      <Modal
        visible={showDetailModal}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>모임 상세정보</Text>
              <TouchableOpacity onPress={() => setShowDetailModal(false)}>
                <Text style={styles.closeButton}>✕</Text>
              </TouchableOpacity>
            </View>
            
            {selectedMeeting && (
              <ScrollView style={styles.modalContent}>
                <Text style={styles.detailTitle}>{selectedMeeting.title}</Text>
                <Text style={styles.detailCategory}>{selectedMeeting.category}</Text>
                
                <Text style={styles.detailDescription}>{selectedMeeting.description}</Text>
                
                <View style={styles.detailInfo}>
                  <Text style={styles.detailInfoText}>📍 {selectedMeeting.location}</Text>
                  <Text style={styles.detailInfoText}>
                    👥 {selectedMeeting.currentParticipants || 0}/{selectedMeeting.maxParticipants}
                  </Text>
                  <Text style={styles.detailInfoText}>
                    🗓️ {new Date(selectedMeeting.meetingDate).toLocaleDateString()}
                  </Text>
                </View>
              </ScrollView>
            )}

            <View style={styles.modalButtons}>
              <TouchableOpacity 
                style={styles.cancelButton}
                onPress={() => setShowDetailModal(false)}
              >
                <Text style={styles.cancelButtonText}>닫기</Text>
              </TouchableOpacity>
              {selectedMeeting && (
                <TouchableOpacity 
                  style={styles.primaryButton}
                  onPress={() => joinMeeting(selectedMeeting.id)}
                >
                  <Text style={styles.primaryButtonText}>참여하기</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    textAlign: 'center',
    marginTop: 50,
    fontSize: 16,
    color: '#666',
  },
  errorText: {
    textAlign: 'center',
    marginTop: 50,
    fontSize: 16,
    color: '#ff4444',
  },
  header: {
    backgroundColor: '#4A90E2',
    padding: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
  },
  createButton: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
  },
  createButtonText: {
    color: '#4A90E2',
    fontWeight: 'bold',
    fontSize: 14,
  },
  content: {
    flex: 1,
    padding: 16,
  },
  emptyContainer: {
    alignItems: 'center',
    marginTop: 100,
  },
  emptyText: {
    fontSize: 16,
    color: '#666',
    marginBottom: 20,
  },
  meetingCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  meetingHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  meetingTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
  },
  meetingCategory: {
    fontSize: 12,
    color: '#4A90E2',
    backgroundColor: '#E8F4F8',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  meetingDescription: {
    fontSize: 14,
    color: '#666',
    marginBottom: 12,
    lineHeight: 20,
  },
  meetingInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  meetingLocation: {
    fontSize: 14,
    color: '#555',
  },
  meetingParticipants: {
    fontSize: 14,
    color: '#555',
  },
  meetingDate: {
    fontSize: 14,
    color: '#555',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContainer: {
    backgroundColor: '#fff',
    borderRadius: 16,
    width: '90%',
    maxHeight: '80%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  closeButton: {
    fontSize: 24,
    color: '#666',
  },
  modalContent: {
    padding: 20,
    maxHeight: 400,
  },
  inputLabel: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
    marginTop: 16,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  multilineInput: {
    height: 80,
    textAlignVertical: 'top',
  },
  categoryContainer: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  categoryChip: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    marginRight: 8,
  },
  categoryChipActive: {
    backgroundColor: '#4A90E2',
  },
  categoryChipText: {
    fontSize: 14,
    color: '#666',
  },
  categoryChipTextActive: {
    color: '#fff',
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    flex: 1,
    marginRight: 8,
  },
  cancelButtonText: {
    textAlign: 'center',
    fontSize: 16,
    color: '#666',
    fontWeight: 'bold',
  },
  primaryButton: {
    backgroundColor: '#4A90E2',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    flex: 1,
    marginLeft: 8,
  },
  primaryButtonText: {
    textAlign: 'center',
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold',
  },
  detailTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  detailCategory: {
    fontSize: 14,
    color: '#4A90E2',
    backgroundColor: '#E8F4F8',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    alignSelf: 'flex-start',
    marginBottom: 16,
  },
  detailDescription: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
    marginBottom: 20,
  },
  detailInfo: {
    backgroundColor: '#f8f8f8',
    padding: 16,
    borderRadius: 8,
  },
  detailInfoText: {
    fontSize: 16,
    color: '#555',
    marginBottom: 8,
  },
});