import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  Image,
  TextInput,
  TouchableOpacity,
  FlatList,
} from 'react-native';
import { Feather, Ionicons, FontAwesome } from '@expo/vector-icons';

// 오늘의 베스트 모임 샘플 데이터
const bestGatherings = [
  {
    id: '1',
    image: 'https://placehold.co/280x160/a2e5b1/333?text=Dance+Class',
    title: '다함께 춤을! 즐거운 댄스 교실',
  },
  {
    id: '2',
    image: 'https://placehold.co/280x160/b1a2e5/333?text=Singing',
    title: '추억의 노래 함께 불러요',
  },
  {
    id: '3',
    image: 'https://placehold.co/280x160/e5a2a2/333?text=Hiking',
    title: '가을 산행, 자연과 함께',
  },
];

// 전체 모임 리스트 샘플 데이터
const allGatherings = [
  {
    id: '1',
    image: 'https://placehold.co/100x100/d3d3d3/000?text=Climbing',
    tags: ['50+', '일산 클라이밍 모임'],
    title: '50+ 일산 클라이밍 모임',
    location: '고양시 일산동구',
    members: 32,
    likes: 120,
    views: 520,
  },
  {
    id: '2',
    image: 'https://placehold.co/100x100/c7eec7/000?text=Singing',
    tags: ['5060 함께하자!', '사교클럽'],
    title: '5060 함께하자! 사교클럽',
    location: '마포구',
    members: 86,
    likes: 6,
    views: 410,
  },
    {
    id: '3',
    image: 'https://placehold.co/100x100/eec7c7/000?text=Badminton',
    tags: ['활기찬', '배드민턴'],
    title: '활기찬 주말 배드민턴!',
    location: '강남구',
    members: 15,
    likes: 98,
    views: 880,
  },
];

export default function MainScreen() {
  // 베스트 모임 카드 렌더링
  const renderBestGathering = ({ item }) => (
    <View style={styles.bestCard}>
      <Image source={{ uri: item.image }} style={styles.bestCardImage} />
      <View style={styles.bestCardOverlay}>
        <Text style={styles.bestCardTitle}>{item.title}</Text>
      </View>
    </View>
  );

  // 전체 모임 카드 렌더링
  const renderGathering = ({ item }) => (
    <View style={styles.gatheringCard}>
      <Image source={{ uri: item.image }} style={styles.gatheringCardImage} />
      <View style={styles.gatheringCardContent}>
        <View style={styles.tagContainer}>
          {item.tags.map((tag, index) => (
            <View key={index} style={[styles.tag, index > 0 && {backgroundColor: '#E8F5E9'}]}>
              <Text style={[styles.tagText, index > 0 && {color: '#00C853'}]}>{tag}</Text>
            </View>
          ))}
        </View>
        <Text style={styles.gatheringCardTitle}>{item.title}</Text>
        <Text style={styles.gatheringCardLocation}>{item.location} • {item.members}명</Text>
        <View style={styles.gatheringCardStats}>
           <FontAwesome name="heart" size={12} color="#FF4081" />
           <Text style={styles.statsText}>{item.likes}</Text>
           <Ionicons name="eye" size={14} color="#757575" style={{marginLeft: 8}}/>
           <Text style={styles.statsText}>{item.views}</Text>
        </View>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>

        {/* 오늘의 베스트 모임 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>오늘의 베스트 모임</Text>
          <FlatList
            data={bestGatherings}
            renderItem={renderBestGathering}
            keyExtractor={(item) => item.id}
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ paddingLeft: 20, paddingRight: 10 }}
          />
        </View>

        {/* 검색창 */}
        <View style={styles.searchContainer}>
          <View style={styles.searchInputWrapper}>
            <Feather name="search" size={20} color="#757575" />
            <TextInput
              placeholder="관심있는 모임 검색"
              style={styles.searchInput}
            />
            <TouchableOpacity>
               <Feather name="mic" size={20} color="#757575" />
            </TouchableOpacity>
          </View>
        </View>

        {/* 필터 버튼 */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterContainer}>
            <TouchableOpacity style={[styles.filterButton, styles.activeFilter]}>
                <Text style={[styles.filterText, styles.activeFilterText]}>전체</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.filterButton}>
                <Text style={styles.filterText}>지역별</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.filterButton}>
                <Text style={styles.filterText}>연령별</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.filterButton}>
                <Text style={styles.filterText}>AI 추천</Text>
            </TouchableOpacity>
        </ScrollView>
        
        {/* 정렬 옵션 */}
        <View style={styles.sortContainer}>
            <TouchableOpacity style={styles.sortOption}>
                <Text style={styles.sortText}>최신등록순</Text>
                <Feather name="chevron-down" size={16} color="#757575" />
            </TouchableOpacity>
             <TouchableOpacity style={styles.sortOption}>
                <Text style={styles.sortText}>참여많은순</Text>
                <Feather name="chevron-down" size={16} color="#757575" />
            </TouchableOpacity>
        </View>

        {/* 모임 리스트 */}
        <FlatList
            data={allGatherings}
            renderItem={renderGathering}
            keyExtractor={(item) => item.id}
            scrollEnabled={false} // 중첩 스크롤 방지
            contentContainerStyle={{ paddingHorizontal: 20 }}
            ItemSeparatorComponent={() => <View style={styles.separator} />}
        />
      </ScrollView>
      {/* Bottom Tab Navigator는 _layout.js 파일에서 설정해야 합니다. */}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#fff',
  },
  container: {
    flex: 1,
  },
  // Header
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
  },
  headerIcons: {
    flexDirection: 'row',
  },
  // Section
  section: {
    marginTop: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginLeft: 20,
    marginBottom: 12,
  },
  // Best Gathering Card
  bestCard: {
    width: 280,
    height: 160,
    borderRadius: 12,
    marginRight: 10,
    overflow: 'hidden',
  },
  bestCardImage: {
    width: '100%',
    height: '100%',
  },
  bestCardOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'flex-end',
    padding: 12,
  },
  bestCardTitle: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  // Search
  searchContainer: {
    paddingHorizontal: 20,
    marginTop: 24,
  },
  searchInputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  searchInput: {
    flex: 1,
    marginLeft: 8,
    fontSize: 16,
  },
  // Filters
  filterContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    marginTop: 20,
    marginBottom: 10,
  },
  filterButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: '#EEEEEE',
    marginRight: 8,
  },
  activeFilter: {
    backgroundColor: '#00C853',
  },
  filterText: {
    color: '#333',
    fontWeight: '500',
  },
  activeFilterText: {
    color: '#fff',
  },
  // Sort
  sortContainer: {
      flexDirection: 'row',
      paddingHorizontal: 20,
      marginTop: 10,
      marginBottom: 16,
  },
  sortOption: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: '#F5F5F5',
      paddingHorizontal: 12,
      paddingVertical: 6,
      borderRadius: 6,
      marginRight: 8,
  },
  sortText: {
      marginRight: 4,
      color: '#555'
  },
  // Gathering Card
  gatheringCard: {
    flexDirection: 'row',
    paddingVertical: 12,
  },
  gatheringCardImage: {
    width: 100,
    height: 100,
    borderRadius: 8,
  },
  gatheringCardContent: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'space-around',
  },
  tagContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  tag: {
    backgroundColor: '#00C853',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginRight: 6,
    marginBottom: 4,
  },
  tagText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  gatheringCardTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginTop: 4,
  },
  gatheringCardLocation: {
    fontSize: 14,
    color: '#757575',
    marginTop: 4,
  },
  gatheringCardStats: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: 8,
  },
  statsText: {
      marginLeft: 4,
      fontSize: 12,
      color: '#757575'
  },
  separator: {
      height: 1,
      backgroundColor: '#EEEEEE',
  }
});
