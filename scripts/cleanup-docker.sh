#!/bin/bash

echo "🧹 Docker 환경 정리 시작..."

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}1. 기존 컨테이너 중지 중...${NC}"
docker-compose -f docker-compose.dev.yml down 2>/dev/null || true

echo -e "${YELLOW}2. 관련 컨테이너 모두 중지...${NC}"
docker stop $(docker ps -q --filter "name=project") 2>/dev/null || true
docker stop $(docker ps -q --filter "name=cheongchun") 2>/dev/null || true

echo -e "${YELLOW}3. 관련 컨테이너 모두 삭제...${NC}"
docker rm $(docker ps -aq --filter "name=project") 2>/dev/null || true
docker rm $(docker ps -aq --filter "name=cheongchun") 2>/dev/null || true

echo -e "${YELLOW}4. 사용하지 않는 볼륨 정리...${NC}"
docker volume prune -f
docker-compose -f docker-compose.dev.yml down -v

echo -e "${YELLOW}5. 사용하지 않는 네트워크 정리...${NC}"
docker network prune -f

echo -e "${YELLOW}6. 사용하지 않는 이미지 정리...${NC}"
docker image prune -f

echo -e "${GREEN}✅ Docker 환경 정리 완료!${NC}"
echo ""
echo "이제 다음 명령어로 깨끗하게 시작하세요 :"
echo "docker-compose -f docker-compose.dev.yml up -d"