# check-db.sh - 데이터베이스 확인 스크립트

echo "🗄️ 청춘장터 데이터베이스 확인"
echo "=================================="

# 색상 설정
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# PostgreSQL 연결 확인
echo -e "${YELLOW}1. 데이터베이스 연결 확인${NC}"
if docker-compose -f docker-compose.dev.yml exec -T postgres pg_isready -U devuser > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL 연결됨${NC}"
else
    echo -e "${RED}❌ PostgreSQL 연결 실패${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}2. 전체 사용자 수${NC}"
USER_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -t -c "SELECT COUNT(*) FROM users;" | tr -d ' ')
echo -e "${BLUE}총 사용자 수: ${USER_COUNT}명${NC}"

echo ""
echo -e "${YELLOW}3. 최근 가입한 사용자 5명${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "
SELECT 
    id,
    username,
    email,
    name,
    provider_type,
    created_at
FROM users 
ORDER BY created_at DESC 
LIMIT 5;
"

echo ""
echo -e "${YELLOW}4. 제공자별 사용자 분포${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "
SELECT 
    provider_type,
    COUNT(*) as count
FROM users 
GROUP BY provider_type;
"

echo ""
echo -e "${YELLOW}5. 소셜 계정 정보${NC}"
SOCIAL_COUNT=$(docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -t -c "SELECT COUNT(*) FROM social_accounts;" | tr -d ' ')
echo -e "${BLUE}소셜 계정 수: ${SOCIAL_COUNT}개${NC}"

if [ "$SOCIAL_COUNT" -gt 0 ]; then
    docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "
    SELECT 
        sa.provider,
        u.username,
        u.email,
        sa.created_at
    FROM social_accounts sa
    JOIN users u ON sa.user_id = u.id
    ORDER BY sa.created_at DESC;
    "
fi

echo ""
echo -e "${YELLOW}6. 이메일 인증 상태${NC}"
docker-compose -f docker-compose.dev.yml exec -T postgres psql -U devuser -d cheongchun_dev -c "
SELECT 
    email_verified,
    COUNT(*) as count
FROM users 
GROUP BY email_verified;
"

echo ""
echo -e "${GREEN}=================================="
echo -e "데이터베이스 확인 완료!${NC}"