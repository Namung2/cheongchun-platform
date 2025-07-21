#!/bin/bash

echo "🔐 카카오 OAuth2 로그인 테스트"
echo "============================="

# 색상 설정
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api"

echo -e "${YELLOW}2. 데이터베이스 연결 확인${NC}"
if docker-compose -f docker-compose.dev.yml exec -T postgres pg_isready -U devuser > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL 연결됨${NC}"
else
    echo -e "${RED}❌ PostgreSQL 연결 실패${NC}"
    exit 1
fi

echo -e "${YELLOW}3. 카카오 로그인 설정 확인${NC}"
if [ -z "${KAKAO_CLIENT_ID}" ]; then
    echo -e "${RED}❌ KAKAO_CLIENT_ID 환경변수가 설정되지 않았습니다${NC}"
    echo "     .env.dev 파일에 실제 카카오 앱 키를 설정하세요"
else
    echo -e "${GREEN}✅ 카카오 클라이언트 ID 설정됨${NC}"
fi

echo -e "${YELLOW}4. OAuth2 엔드포인트 테스트${NC}"
echo -e "${BLUE}🎯 브라우저에서 다음 URL들을 테스트하세요:${NC}"
echo ""
echo -e "${GREEN}카카오 로그인 URL 확인:${NC}"
echo -e "${BLUE}$BASE_URL/auth/kakao${NC}"
echo ""
echo -e "${GREEN}카카오 로그인 시작:${NC}"
echo -e "${BLUE}http://localhost:8080/oauth2/authorization/kakao${NC}"
echo ""
echo -e "${GREEN}로그인 상태 확인:${NC}"
echo -e "${BLUE}$BASE_URL/auth/me${NC}"

echo ""
echo -e "${YELLOW}📋 카카오 로그인 테스트 절차:${NC}"
echo "1. 위의 '카카오 로그인 시작' URL을 브라우저에서 열기"
echo "2. 카카오 계정으로 로그인"
echo "3. 앱 권한 동의"
echo "4. 카카오가 http://localhost:8080/oauth2/callback/kakao로 리다이렉트"
echo "5. Spring Security가 자동 처리 후 $BASE_URL/auth/oauth2/success 페이지로 이동"
echo "6. JSON 응답에서 JWT 토큰 확인"
echo ""
echo -e "${YELLOW}🔍 문제 해결:${NC}"
echo "- 카카오 개발자 콘솔 Redirect URI: http://localhost:8080/oauth2/callback/kakao"
echo "- application.properties에 카카오 클라이언트 정보 확인"
echo "- 브라우저 콘솔에서 에러 메시지 확인"