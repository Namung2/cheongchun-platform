import React from 'react';
import { Image, View, TouchableOpacity } from 'react-native';
import { Tabs, useRouter } from 'expo-router';
import { Ionicons, Feather, FontAwesome } from '@expo/vector-icons';
import blackIcon from '../../assets/images/검은글씨표지.png';

export default function TabLayout() {
  const router = useRouter();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#00C853',
        tabBarShowLabel: true,
        headerTitleAlign: 'center',

        // 상단 바 왼쪽: 뒤로가기 버튼
        headerLeft: () => (
          <TouchableOpacity onPress={() => router.back()} style={{ marginLeft: 15 }}>
            <Ionicons name="arrow-back" size={24} color="black" />
          </TouchableOpacity>
        ),

        // 상단 바 중앙: 로고 이미지
        headerTitle: () => (
          <Image
            source={ blackIcon }
            style={{ width: 290, height: 220, resizeMode: 'contain', marginTop: 5 }}
          />
        ),

        // 상단 바 오른쪽: 검색 및 알림 버튼
        headerRight: () => (
          <View style={{ flexDirection: 'row'}}>
            <TouchableOpacity style={{ marginRight: 10 }}>
              <Feather name="search" size={24} color="black" />
            </TouchableOpacity>
            <TouchableOpacity style={{ marginRight: 10}}>
              <FontAwesome name="bell-o" size={24} color="black" />
            </TouchableOpacity>
          </View>
        ),
      }}
    >
      <Tabs.Screen
        name="main" // app/(tabs)/main.js 파일
        options={{
          title: '모임',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="home" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="explore" // app/(tabs)/explore.js 파일
        options={{
          title: '탐색',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="rocket-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="chat" // app/(tabs)/chat.js 파일
        options={{
          title: '채팅',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="chatbubble-ellipses-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="ai" // app/(tabs)/ai.js 파일
        options={{
          title: 'AI봇',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="sparkles-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="mypage" // app/(tabs)/mypage.js 파일
        options={{
          title: '마이페이지',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="person-outline" size={size} color={color} />
          ),
        }}
      />
    </Tabs>
  );
}

