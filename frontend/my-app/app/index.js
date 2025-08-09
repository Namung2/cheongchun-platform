import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import { useAuth } from '../hooks/useAuth';

export default function Index() {
  const router = useRouter();
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (!loading) {
      if (isAuthenticated) {
        router.replace('/main');
      } else {
        router.replace('/splash');
      }
    }
  }, [isAuthenticated, loading]);

  return null; // 로딩 중이므로 아무것도 렌더링하지 않음
}