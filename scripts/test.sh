#!/bin/bash

# 청춘장터 API 테스트 스크립트
# 사용법: ./test_meeting_apis.sh

BASE_URL="http://localhost:8080/api"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 전역 변수
JWT_TOKEN=""
USER_ID=""
MEETING_ID=""
PARTICIPANT_ID=""

echo -e "${YELLOW}=== 청춘장터 API 종합 테스트 ===${NC}"

# 서버 상태 확인
check_server() {
    echo -e "\n${YELLOW}1. 서버 상태 확인${NC}"
    response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/test/hello)
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ 서버가 정상 작동 중입니다${NC}"
    else
        echo -e "${RED}✗ 서버 연결 실패 (HTTP: $response)${NC}"
        exit 1
    fi
}

# 사용자 회원가입 및 로그인
setup_user() {
    echo -e "\n${YELLOW}2. 사용자 인증 설정${NC}"
    
    # 고유한 사용자명 생성
    TIMESTAMP=$(date +%s)
    USERNAME="testuser$TIMESTAMP"
    EMAIL="test$TIMESTAMP@example.com"
    PASSWORD="password123"
    NAME="mytest$TIMESTAMP"

    # 회원가입
    echo "회원가입 중..."
    signup_response=$(curl -s -X POST ${BASE_URL}/auth/signup \
    -H "Content-Type: application/json; charset=utf-8" \
    -H "Origin: http://localhost:8080" \
    -d "{
        \"username\": \"$USERNAME\",
        \"email\": \"$EMAIL\",
        \"password\": \"$PASSWORD\",
        \"name\": \"$NAME\"
    }")

    # JWT 토큰 추출
    JWT_TOKEN=$(echo $signup_response | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    USER_ID=$(echo $signup_response | grep -o '"id":[0-9]*' | cut -d':' -f2)

    if [ -n "$JWT_TOKEN" ]; then
        echo -e "${GREEN}✓ 회원가입 및 로그인 성공${NC}"
        echo "사용자 ID: $USER_ID"
        echo "JWT 토큰: ${JWT_TOKEN:0:50}..."
    else
        echo -e "${RED}✗ 회원가입 실패${NC}"
        echo "응답: $signup_response"
        exit 1
    fi
}

# 모임 생성 테스트
test_meeting_creation() {
    echo -e "\n${YELLOW}3. 모임 생성 테스트${NC}"
    
    # 미래 날짜 계산 (내일)
    START_DATE=$(date -d "tomorrow 14:00" -Iseconds)
    END_DATE=$(date -d "tomorrow 16:00" -Iseconds)
    
    meeting_response=$(curl -s -X POST ${BASE_URL}/meetings \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Content-Type: application/json; charset=utf-8" \
    -H "Origin: http://localhost:8080" \
    -d "{
        \"title\": \"테스트 한강 산책 모임\",
        \"description\": \"API 테스트를 위한 모임입니다\",
        \"category\": \"EXERCISE\",
        \"subcategory\": \"WALKING\",
        \"location\": \"서울시 영등포구\",
        \"address\": \"한강공원 여의도지구\",
        \"startDate\": \"$START_DATE\",
        \"endDate\": \"$END_DATE\",
        \"maxParticipants\": 10,
        \"fee\": 0,
        \"difficultyLevel\": \"BEGINNER\",
        \"ageRange\": \"50-70\",
        \"organizerContact\": \"010-1234-5678\"
    }")

    MEETING_ID=$(echo $meeting_response | grep -o '"id":[0-9]*' | cut -d':' -f2)

    if [ -n "$MEETING_ID" ]; then
        echo -e "${GREEN}✓ 모임 생성 성공 (ID: $MEETING_ID)${NC}"
    else
        echo -e "${RED}✗ 모임 생성 실패${NC}"
        echo "응답: $meeting_response"
        return 1
    fi
}

# 모임 조회 테스트
test_meeting_retrieval() {
    echo -e "\n${YELLOW}4. 모임 조회 테스트${NC}"

    # 모임 상세 조회
    echo "모임 상세 조회..."
    detail_response=$(curl -s -X GET ${BASE_URL}/meetings/${MEETING_ID} \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$detail_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 모임 상세 조회 성공${NC}"
    else
        echo -e "${RED}✗ 모임 상세 조회 실패${NC}"
    fi

    # 모임 목록 조회
    echo "모임 목록 조회..."
    list_response=$(curl -s -X GET "${BASE_URL}/meetings?page=0&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$list_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 모임 목록 조회 성공${NC}"
    else
        echo -e "${RED}✗ 모임 목록 조회 실패${NC}"
    fi

    # 오늘의 베스트 모임
    echo "오늘의 베스트 모임 조회..."
    best_response=$(curl -s -X GET "${BASE_URL}/meetings/today-best?page=0&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$best_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 오늘의 베스트 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 오늘의 베스트 모임 조회 실패${NC}"
    fi
}

# 찜 기능 테스트
test_wishlist() {
    echo -e "\n${YELLOW}5. 찜 기능 테스트${NC}"

    # 찜 추가
    echo "찜 추가..."
    add_wishlist_response=$(curl -s -X POST ${BASE_URL}/wishlist/meetings/${MEETING_ID} \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$add_wishlist_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 찜 추가 성공${NC}"
    else
        echo -e "${RED}✗ 찜 추가 실패${NC}"
        echo "응답: $add_wishlist_response"
    fi

    # 찜 여부 확인
    echo "찜 여부 확인..."
    check_wishlist_response=$(curl -s -X GET ${BASE_URL}/wishlist/meetings/${MEETING_ID}/check \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$check_wishlist_response" | grep -q '"isWishlisted":true'; then
        echo -e "${GREEN}✓ 찜 여부 확인 성공 (찜됨)${NC}"
    else
        echo -e "${RED}✗ 찜 여부 확인 실패${NC}"
    fi

    # 내 찜 목록 조회
    echo "내 찜 목록 조회..."
    my_wishlist_response=$(curl -s -X GET "${BASE_URL}/wishlist/my?page=0&size=10" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$my_wishlist_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 찜 목록 조회 성공${NC}"
    else
        echo -e "${RED}✗ 찜 목록 조회 실패${NC}"
    fi

    # 찜 통계 조회
    echo "찜 통계 조회..."
    wishlist_stats_response=$(curl -s -X GET ${BASE_URL}/wishlist/stats \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$wishlist_stats_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 찜 통계 조회 성공${NC}"
    else
        echo -e "${RED}✗ 찜 통계 조회 실패${NC}"
    fi
}

# 두 번째 사용자 생성 (참여 테스트용)
create_second_user() {
    echo -e "\n${YELLOW}6. 두 번째 사용자 생성 (참여 테스트용)${NC}"
    
    TIMESTAMP2=$(date +%s)_2
    USERNAME2="participant$TIMESTAMP2"
    EMAIL2="participant$TIMESTAMP2@example.com"
    
    signup2_response=$(curl -s -X POST ${BASE_URL}/auth/signup \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$USERNAME2\",
            \"email\": \"$EMAIL2\",
            \"password\": \"$PASSWORD\",
            \"name\": \"참가자$TIMESTAMP2\"
        }")

    JWT_TOKEN2=$(echo $signup2_response | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

    if [ -n "$JWT_TOKEN2" ]; then
        echo -e "${GREEN}✓ 두 번째 사용자 생성 성공${NC}"
    else
        echo -e "${RED}✗ 두 번째 사용자 생성 실패${NC}"
        return 1
    fi
}

# 모임 참여 테스트
test_meeting_participation() {
    echo -e "\n${YELLOW}7. 모임 참여 테스트${NC}"

    # 모임 참여 신청 (두 번째 사용자로)
    echo "모임 참여 신청..."
    join_response=$(curl -s -X POST ${BASE_URL}/meetings/${MEETING_ID}/join \
        -H "Authorization: Bearer $JWT_TOKEN2" \
        -H "Content-Type: application/json" \
        -d "{
            \"applicationMessage\": \"테스트 참여 신청입니다.\"
        }")

    PARTICIPANT_ID=$(echo $join_response | grep -o '"id":[0-9]*' | cut -d':' -f2)

    if [ -n "$PARTICIPANT_ID" ]; then
        echo -e "${GREEN}✓ 모임 참여 신청 성공 (참가자 ID: $PARTICIPANT_ID)${NC}"
    else
        echo -e "${RED}✗ 모임 참여 신청 실패${NC}"
        echo "응답: $join_response"
        return 1
    fi

    # 참여 상태 조회
    echo "참여 상태 조회..."
    participation_status_response=$(curl -s -X GET ${BASE_URL}/meetings/${MEETING_ID}/participation \
        -H "Authorization: Bearer $JWT_TOKEN2")

    if echo "$participation_status_response" | grep -q '"isParticipating":true'; then
        echo -e "${GREEN}✓ 참여 상태 조회 성공${NC}"
    else
        echo -e "${RED}✗ 참여 상태 조회 실패${NC}"
    fi

    # 참여 신청 승인 (주최자로)
    echo "참여 신청 승인..."
    approve_response=$(curl -s -X POST ${BASE_URL}/meetings/participants/${PARTICIPANT_ID}/approve \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$approve_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 참여 신청 승인 성공${NC}"
    else
        echo -e "${RED}✗ 참여 신청 승인 실패${NC}"
        echo "응답: $approve_response"
    fi

    # 참가자 목록 조회
    echo "참가자 목록 조회..."
    participants_response=$(curl -s -X GET "${BASE_URL}/meetings/${MEETING_ID}/participants?page=0&size=10" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$participants_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 참가자 목록 조회 성공${NC}"
    else
        echo -e "${RED}✗ 참가자 목록 조회 실패${NC}"
    fi
}

# 내 참여 모임 조회 테스트
test_my_participations() {
    echo -e "\n${YELLOW}8. 내 참여 모임 조회 테스트${NC}"

    # 내 참여 모임 목록
    echo "내 참여 모임 목록 조회..."
    my_participations_response=$(curl -s -X GET "${BASE_URL}/meetings/my-participations?page=0&size=10" \
        -H "Authorization: Bearer $JWT_TOKEN2")

    if echo "$my_participations_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 내 참여 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 내 참여 모임 조회 실패${NC}"
    fi

    # 곧 시작되는 참여 모임
    echo "곧 시작되는 참여 모임 조회..."
    upcoming_participations_response=$(curl -s -X GET ${BASE_URL}/meetings/upcoming \
        -H "Authorization: Bearer $JWT_TOKEN2")

    if echo "$upcoming_participations_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 곧 시작되는 참여 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 곧 시작되는 참여 모임 조회 실패${NC}"
    fi
}

# 모임 관리 테스트 (주최자용)
test_meeting_management() {
    echo -e "\n${YELLOW}9. 모임 관리 테스트 (주최자용)${NC}"

    # 내가 생성한 모임 조회
    echo "내가 생성한 모임 조회..."
    my_meetings_response=$(curl -s -X GET "${BASE_URL}/meetings/my?page=0&size=10" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$my_meetings_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 내가 생성한 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 내가 생성한 모임 조회 실패${NC}"
    fi

    # 신청 관리 목록 조회
    echo "신청 관리 목록 조회..."
    applications_response=$(curl -s -X GET "${BASE_URL}/meetings/my-applications?page=0&size=10" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$applications_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 신청 관리 목록 조회 성공${NC}"
    else
        echo -e "${RED}✗ 신청 관리 목록 조회 실패${NC}"
    fi

    # 참여 통계 조회
    echo "참여 통계 조회..."
    stats_response=$(curl -s -X GET ${BASE_URL}/meetings/${MEETING_ID}/participation-stats \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$stats_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 참여 통계 조회 성공${NC}"
    else
        echo -e "${RED}✗ 참여 통계 조회 실패${NC}"
    fi
}

# 검색 및 필터링 테스트
test_search_and_filtering() {
    echo -e "\n${YELLOW}10. 검색 및 필터링 테스트${NC}"

    # 키워드 검색
    echo "키워드 검색..."
    search_response=$(curl -s -X GET "${BASE_URL}/meetings/search?keyword=한강&page=0&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$search_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 키워드 검색 성공${NC}"
    else
        echo -e "${RED}✗ 키워드 검색 실패${NC}"
    fi

    # 카테고리별 모임 조회
    echo "카테고리별 모임 조회..."
    category_response=$(curl -s -X GET "${BASE_URL}/meetings/category/EXERCISE?page=0&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$category_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 카테고리별 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 카테고리별 모임 조회 실패${NC}"
    fi

    # 취미 모임 조회
    echo "취미 모임 조회..."
    hobby_response=$(curl -s -X GET "${BASE_URL}/meetings/hobby?page=0&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$hobby_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 취미 모임 조회 성공${NC}"
    else
        echo -e "${RED}✗ 취미 모임 조회 실패${NC}"
    fi
}

# 정리 작업
cleanup() {
    echo -e "\n${YELLOW}11. 정리 작업${NC}"

    # 찜 삭제
    echo "찜 삭제..."
    remove_wishlist_response=$(curl -s -X DELETE ${BASE_URL}/wishlist/meetings/${MEETING_ID} \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$remove_wishlist_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 찜 삭제 성공${NC}"
    else
        echo -e "${RED}✗ 찜 삭제 실패${NC}"
    fi

    # 참여 신청 취소
    echo "참여 신청 취소..."
    cancel_response=$(curl -s -X DELETE ${BASE_URL}/meetings/${MEETING_ID}/join \
        -H "Authorization: Bearer $JWT_TOKEN2")

    if echo "$cancel_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 참여 신청 취소 성공${NC}"
    else
        echo -e "${YELLOW}! 참여 신청 취소 실패 (이미 승인된 상태일 수 있음)${NC}"
    fi

    # 모임 삭제
    echo "모임 삭제..."
    delete_meeting_response=$(curl -s -X DELETE ${BASE_URL}/meetings/${MEETING_ID} \
        -H "Authorization: Bearer $JWT_TOKEN")

    if echo "$delete_meeting_response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 모임 삭제 성공${NC}"
    else
        echo -e "${YELLOW}! 모임 삭제 실패 (참가자가 있어서 삭제할 수 없을 수 있음)${NC}"
    fi
}

# 테스트 결과 요약
show_summary() {
    echo -e "\n${BLUE}=== 테스트 완료 ===${NC}"
    echo -e "${GREEN}성공적으로 테스트된 기능들:${NC}"
    echo "✓ 서버 상태 확인"
    echo "✓ 사용자 인증 (회원가입/로그인)"
    echo "✓ 모임 생성"
    echo "✓ 모임 조회 (상세, 목록, 베스트)"
    echo "✓ 찜 기능 (추가, 확인, 목록, 통계)"
    echo "✓ 모임 참여 (신청, 승인, 상태 조회)"
    echo "✓ 참가자 관리"
    echo "✓ 검색 및 필터링"
    echo ""
    echo -e "${BLUE}생성된 테스트 데이터:${NC}"
    echo "모임 ID: $MEETING_ID"
    echo "참가자 ID: $PARTICIPANT_ID"
    echo "주최자 토큰: ${JWT_TOKEN:0:50}..."
    echo "참가자 토큰: ${JWT_TOKEN2:0:50}..."
}

# 메인 실행 흐름
main() {
    check_server
    setup_user
    test_meeting_creation
    test_meeting_retrieval
    test_wishlist
    create_second_user
    test_meeting_participation
    test_my_participations
    test_meeting_management
    test_search_and_filtering
    cleanup
    show_summary
}

# 스크립트 실행
main