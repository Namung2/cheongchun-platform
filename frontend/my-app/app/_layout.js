// app/_layout.js
import { Stack } from 'expo-router';
import { useEffect, useState } from 'react';

export default function RootLayout() {
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    // Root Layout 마운트 완료 시그널
    setIsReady(true);
  }, []);

  if (!isReady) {
    return null; // 또는 로딩 스피너
  }

  return (
    <Stack
      screenOptions={{ headerShown: false }}
    />
  );
}