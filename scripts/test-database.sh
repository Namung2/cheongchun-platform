#!/bin/bash

echo "🗄️  PostgreSQL 데이터베이스 테스트"
echo "================================"

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

# 3. 확장 기능 확인
echo -e "${YELLOW}3. 확장 기능 확인${NC}"
EXTENSIONS=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT extname FROM pg_extension WHERE extname IN ('uuid-ossp', 'pgcrypto', 'vector');" 2>/dev/null | grep -E "(uuid-ossp|pgcrypto|vector)" | wc -l)
echo -e "${BLUE}🔧 설치된 확장 기능: ${EXTENSIONS}개 (uuid-ossp, pgcrypto, vector)${NC}"

# 4. 테이블 존재 확인
echo -e "${YELLOW}4. 테이블 존재 확인${NC}"
TABLES=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "\dt" | grep "public" | wc -l)
echo -e "${BLUE}📊 총 ${TABLES}개의 테이블이 존재합니다${NC}"

# 5. 각 테이블별 데이터 확인
echo -e "${YELLOW}5. 테이블별 데이터 확인${NC}"

# Users 테이블 (username 필드 포함)
USER_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM users;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}👥 Users: ${USER_COUNT}명${NC}"

# Meetings 테이블  
MEETING_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM meetings;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}🤝 Meetings: ${MEETING_COUNT}개${NC}"

# User Interests 테이블
INTEREST_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM user_interests;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}🎯 User Interests: ${INTEREST_COUNT}개${NC}"

# Wishlists 테이블
WISHLIST_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM user_wishlists;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}💖 Wishlists: ${WISHLIST_COUNT}개${NC}"

# 6. 샘플 데이터 조회 (username 필드 포함)
echo -e "${YELLOW}6. 샘플 데이터 조회${NC}"
echo -e "${BLUE}📋 사용자 목록 (username 포함):${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT id, username, email, name, created_at FROM users LIMIT 3;"

echo -e "${BLUE}📋 모임 목록:${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT id, title, category, location, created_by FROM meetings LIMIT 3;"

echo -e "${BLUE}📋 사용자 관심사:${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT u.username, ui.category, ui.interest FROM user_interests ui JOIN users u ON ui.user_id = u.id LIMIT 5;"

# 7. 인덱스 확인
echo -e "${YELLOW}7. 인덱스 확인${NC}"
INDEX_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM pg_indexes WHERE tablename IN ('users', 'meetings', 'user_interests', 'user_wishlists');" 2>/dev/null | grep -o '[0-9]\+' | head -1)
echo -e "${BLUE}🔍 생성된 인덱스: ${INDEX_COUNT}개${NC}"

# 8. Vector 확장 기능 테스트
echo -e "${YELLOW}8. Vector 확장 기능 테스트${NC}"
VECTOR_TEST=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM pg_type WHERE typname = 'vector';" 2>/dev/null | grep -o '[0-9]\+' | head -1)
if [ "$VECTOR_TEST" -eq "1" ]; then
    echo -e "${GREEN}✅ Vector 확장 기능 정상 작동${NC}"
else
    echo -e "${RED}❌ Vector 확장 기능 미설치${NC}"
fi

# 9. 데이터 무결성 확인
echo -e "${YELLOW}9. 데이터 무결성 확인${NC}"

# 사용자명 중복 확인
USERNAME_DUPLICATES=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM (SELECT username FROM users GROUP BY username HAVING COUNT(*) > 1) AS duplicates;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
if [ "$USERNAME_DUPLICATES" -eq "0" ]; then
    echo -e "${GREEN}✅ 사용자명 중복 없음${NC}"
else
    echo -e "${RED}❌ 사용자명 중복 발견: ${USERNAME_DUPLICATES}개${NC}"
fi

# 이메일 중복 확인
EMAIL_DUPLICATES=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "SELECT COUNT(*) FROM (SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1) AS duplicates;" 2>/dev/null | grep -o '[0-9]\+' | head -1)
if [ "$EMAIL_DUPLICATES" -eq "0" ]; then
    echo -e "${GREEN}✅ 이메일 중복 없음${NC}"
else
    echo -e "${RED}❌ 이메일 중복 발견: ${EMAIL_DUPLICATES}개${NC}"
fi

# 10. 테스트 계정 정보 출력
echo -e "${YELLOW}10. 테스트 계정 정보${NC}"
echo -e "${PURPLE}📱 로그인 테스트용 계정:${NC}"
echo -e "${BLUE}  username: cheolsu  | email: test@cheongchun.com${NC}"
echo -e "${BLUE}  username: admin    | email: admin@cheongchun.com${NC}"
echo -e "${BLUE}  username: younghee | email: user1@example.com${NC}"
echo -e "${BLUE}  username: minsu    | email: user2@example.com${NC}"

# 11. 인터랙티브 접속 안내
echo ""
echo -e "${GREEN}✅ 모든 테스트 완료!${NC}"
echo ""
echo -e "${YELLOW}🎯 직접 데이터베이스에 접속하려면:${NC}"
echo -e "${BLUE}docker-compose -f docker-compose.dev.yml exec postgres psql -U devuser -d cheongchun_dev${NC}"
echo ""
echo -e "${YELLOW}📚 유용한 SQL 명령어:${NC}"
echo -e "${BLUE}\\dt                    ${NC}# 테이블 목록"
echo -e "${BLUE}\\d users               ${NC}# users 테이블 구조"
echo -e "${BLUE}\\d meetings            ${NC}# meetings 테이블 구조"
echo -e "${BLUE}SELECT * FROM users;    ${NC}# 모든 사용자 조회"
echo -e "${BLUE}SELECT * FROM meetings; ${NC}# 모든 모임 조회"
echo -e "${BLUE}\\q                     ${NC}# 종료"
echo ""
echo -e "${YELLOW}🧪 데이터베이스 테스트 명령어:${NC}"
echo -e "${BLUE}-- 사용자명으로 검색${NC}"
echo -e "${BLUE}SELECT * FROM users WHERE username = 'cheolsu';${NC}"
echo -e "${BLUE}-- 이메일로 검색${NC}"
echo -e "${BLUE}SELECT * FROM users WHERE email = 'test@cheongchun.com';${NC}"
echo -e "${BLUE}-- 사용자별 관심사 조회${NC}"
echo -e "${BLUE}SELECT u.username, ui.category, ui.interest FROM users u JOIN user_interests ui ON u.id = ui.user_id;${NC}"
echo -e "${BLUE}-- 모임 생성자 정보 조회${NC}"
echo -e "${BLUE}SELECT m.title, u.username, u.name FROM meetings m JOIN users u ON m.created_by = u.id;${NC}"