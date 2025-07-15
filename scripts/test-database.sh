#!/bin/bash

echo "🗄️  PostgreSQL 데이터베이스 테스트"
echo "================================"

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 1. 컨테이너 상태 확인
echo -e "${YELLOW}1. 컨테이너 상태 확인${NC}"
if docker-compose -f docker-compose.dev.yml ps postgres | grep -q "Up"; then
    echo -e "${GREEN}✅ PostgreSQL 컨테이너 실행 중${NC}"
else
    echo -e "${RED}❌ PostgreSQL 컨테이너가 실행되지 않았습니다${NC}"
    echo "먼저 docker-compose -f docker-compose.dev.yml up -d 를 실행하세요"
    exit 1
fi

# 2. 기본 연결 테스트
echo -e "${YELLOW}2. 데이터베이스 연결 테스트${NC}"
if docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 데이터베이스 연결 성공${NC}"
else
    echo -e "${RED}❌ 데이터베이스 연결 실패${NC}"
    exit 1
fi

# 3. 테이블 존재 확인
echo -e "${YELLOW}3. 테이블 존재 확인${NC}"
TABLES=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "\dt" | grep "public" | wc -l)
echo -e "${BLUE}📊 총 ${TABLES}개의 테이블이 존재합니다${NC}"

# 4. 각 테이블별 데이터 확인
echo -e "${YELLOW}4. 테이블별 데이터 확인${NC}"

# Users 테이블
USER_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM users;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}👥 Users: ${USER_COUNT}명${NC}"

# Meetings 테이블  
MEETING_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM meetings;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}🤝 Meetings: ${MEETING_COUNT}개${NC}"

# 5. 샘플 데이터 조회
echo -e "${YELLOW}5. 샘플 데이터 조회${NC}"
echo -e "${BLUE}📋 사용자 목록:${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT id, email, name, created_at FROM users LIMIT 3;"

echo -e "${BLUE}📋 모임 목록:${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT id, title, category, location FROM meetings LIMIT 3;"

# 6. 인터랙티브 접속 안내
echo ""
echo -e "${GREEN}✅ 모든 테스트 완료!${NC}"
echo ""
echo -e "${YELLOW}🎯 직접 데이터베이스에 접속하려면:${NC}"
echo -e "${BLUE}docker-compose -f docker-compose.dev.yml exec postgres psql -U devuser -d cheongchun_dev${NC}"
echo ""
echo -e "${YELLOW}📚 유용한 SQL 명령어:${NC}"
echo -e "${BLUE}\\dt          ${NC}# 테이블 목록"
echo -e "${BLUE}\\d users     ${NC}# users 테이블 구조"  
echo -e "${BLUE}SELECT * FROM users;${NC}  # 모든 사용자 조회"
echo -e "${BLUE}\\q           ${NC}# 종료"