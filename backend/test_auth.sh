#!/bin/bash

# 인증 시스템 테스트 스크립트
# 사용법: ./test_auth.sh

BASE_URL="http://localhost:8080/api"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== 청춘장터 인증 시스템 테스트 ===${NC}"

# 서버 상태 확인
echo -e "\n${YELLOW}1. 서버 상태 확인${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/auth/test)
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ 서버가 정상 작동 중입니다${NC}"
else
    echo -e "${RED}✗ 서버 연결 실패 (HTTP: $response)${NC}"
    exit 1
fi

# 테스트 사용자 데이터
USERNAME="testuser$(date +%s)"
EMAIL="test$(date +%s)@example.com"
PASSWORD="password123"
NAME="테스트 사용자"

echo -e "\n${YELLOW}2. 회원가입 테스트${NC}"
signup_response=$(curl -s -X POST ${BASE_URL}/auth/signup \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"$USERNAME\",
        \"email\": \"$EMAIL\",
        \"password\": \"$PASSWORD\",
        \"name\": \"$NAME\"
    }")

echo "회원가입 응답: $signup_response"

# JWT 토큰 추출
JWT_TOKEN=$(echo $signup_response | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$JWT_TOKEN" ]; then
    echo -e "${GREEN}✓ 회원가입 성공${NC}"
    echo "JWT 토큰: ${JWT_TOKEN:0:50}..."
else
    echo -e "${RED}✗ 회원가입 실패${NC}"
    exit 1
fi

echo -e "\n${YELLOW}3. 로그인 테스트${NC}"
login_response=$(curl -s -X POST ${BASE_URL}/auth/login \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"$USERNAME\",
        \"password\": \"$PASSWORD\"
    }")

echo "로그인 응답: $login_response"

# 새 JWT 토큰 추출
NEW_JWT_TOKEN=$(echo $login_response | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$NEW_JWT_TOKEN" ]; then
    echo -e "${GREEN}✓ 로그인 성공${NC}"
    JWT_TOKEN=$NEW_JWT_TOKEN
else
    echo -e "${RED}✗ 로그인 실패${NC}"
    exit 1
fi

echo -e "\n${YELLOW}4. 현재 사용자 정보 조회 테스트${NC}"
me_response=$(curl -s -X GET ${BASE_URL}/auth/me \
    -H "Authorization: Bearer $JWT_TOKEN")

echo "사용자 정보 응답: $me_response"

if echo "$me_response" | grep -q '"success":true'; then
    echo -e "${GREEN}✓ 사용자 정보 조회 성공${NC}"
else
    echo -e "${RED}✗ 사용자 정보 조회 실패${NC}"
fi

echo -e "\n${YELLOW}5. 잘못된 토큰으로 접근 테스트${NC}"
invalid_response=$(curl -s -X GET ${BASE_URL}/auth/me \
    -H "Authorization: Bearer invalid_token")

echo "잘못된 토큰 응답: $invalid_response"

if echo "$invalid_response" | grep -q '"success":false'; then
    echo -e "${GREEN}✓ 잘못된 토큰 차단 성공${NC}"
else
    echo -e "${RED}✗ 잘못된 토큰 차단 실패${NC}"
fi

echo -e "\n${YELLOW}6. 중복 사용자명 테스트${NC}"
duplicate_response=$(curl -s -X POST ${BASE_URL}/auth/signup \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"$USERNAME\",
        \"email\": \"another$EMAIL\",
        \"password\": \"$PASSWORD\",
        \"name\": \"$NAME\"
    }")

echo "중복 사용자명 응답: $duplicate_response"

if echo "$duplicate_response" | grep -q '"success":false'; then
    echo -e "${GREEN}✓ 중복 사용자명 차단 성공${NC}"
else
    echo -e "${RED}✗ 중복 사용자명 차단 실패${NC}"
fi

echo -e "\n${YELLOW}7. 로그아웃 테스트${NC}"
logout_response=$(curl -s -X POST ${BASE_URL}/auth/logout \
    -H "Authorization: Bearer $JWT_TOKEN")

echo "로그아웃 응답: $logout_response"

if echo "$logout_response" | grep -q '"success":true'; then
    echo -e "${GREEN}✓ 로그아웃 성공${NC}"
else
    echo -e "${RED}✗ 로그아웃 실패${NC}"
fi

echo -e "\n${GREEN}=== 테스트 완료 ===${NC}"
echo -e "생성된 테스트 사용자:"
echo -e "  사용자명: $USERNAME"
echo -e "  이메일: $EMAIL"
echo -e "  비밀번호: $PASSWORD"