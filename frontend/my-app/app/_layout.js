// app/_layout.js
import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack
      initialRouteName="splash"
      screenOptions={{ headerShown: false }}
    />
  );
}