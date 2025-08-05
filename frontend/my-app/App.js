import { registerRootComponent } from 'expo';
import { Slot } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

export default function App() {
  return (
    <>
      <Slot /> 
      <StatusBar style="auto" />
    </>
  );
}

// Expo에서 앱 시작점 등록
registerRootComponent(App);