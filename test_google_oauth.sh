#!/bin/bash

# Google OAuth 테스트 스크립트
# WSL + Android 에뮬레이터 환경에서 Google 로그인 디버깅

echo "🔍 Google OAuth 테스트 및 디버깅 스크립트"
echo "========================================="

# 1. Android 에뮬레이터 상태 확인
echo "1. Android 에뮬레이터 상태 확인..."
adb devices

# 2. 앱이 설치되어 있는지 확인
echo -e "\n2. 앱 패키지 확인..."
adb shell pm list packages | grep myapp || echo "앱이 설치되지 않았습니다"

# 3. 딥링크 테스트 (수동)
echo -e "\n3. 딥링크 테스트 함수 정의..."
echo "딥링크를 테스트하려면 다음 명령어를 실행하세요:"
echo "test_deeplink() {"
echo "  adb shell am start \\"
echo "    -W -a android.intent.action.VIEW \\"
echo "    -d \"myapp://auth-success?token=test_token&userId=123&email=test%40example.com&name=Test%20User\" \\"
echo "    expo.modules.devlauncher/.MainActivity"
echo "}"
echo ""
echo "사용법: test_deeplink"

# 4. 로그 모니터링
echo -e "\n4. 로그 모니터링 시작..."
echo "Android 로그를 모니터링합니다. Ctrl+C로 중단하세요."
echo "========================================="

# 앱 관련 로그만 필터링
adb logcat | grep -E "(myapp|expo|OAuth|auth-success|Deep link)"