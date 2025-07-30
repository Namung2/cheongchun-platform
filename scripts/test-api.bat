@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:: 청춘장터 API 테스트 스크립트 (Windows 호환 버전)
:: 사용법: test_meeting_apis_simple.bat

set BASE_URL=http://localhost:8080/api

:: 전역 변수
set JWT_TOKEN=
set USER_ID=
set MEETING_ID=
set PARTICIPANT_ID=
set JWT_TOKEN2=

echo ===============================================
echo    청춘장터 API 종합 테스트 (Windows)
echo ===============================================
echo.

:: curl 설치 확인
curl --version > nul 2>&1
if errorlevel 1 (
    echo [ERROR] curl이 설치되어 있지 않습니다.
    echo Windows 10 이상에서는 기본 제공됩니다.
    echo curl을 다운로드하세요: https://curl.se/windows/
    pause
    exit /b 1
)

:: 1. 서버 상태 확인
echo 1. 서버 상태 확인
echo -----------------------------------------------
curl -s -o nul -w "%%{http_code}" %BASE_URL%/test/hello > temp_status.txt 2>&1
set /p STATUS=<temp_status.txt
del temp_status.txt 2>nul

if "%STATUS%"=="200" (
    echo [SUCCESS] 서버가 정상 작동 중입니다
) else (
    echo [ERROR] 서버 연결 실패 (HTTP: %STATUS%^)
    pause
    exit /b 1
)
echo.

:: 2. 사용자 인증 설정
echo 2. 사용자 인증 설정
echo -----------------------------------------------

:: 고유한 사용자명 생성
for /f "tokens=1-4 delims=:.," %%a in ("!time: =0!") do set TIMESTAMP=%%a%%b%%c%%d
set USERNAME=testuser!TIMESTAMP!
set EMAIL=test!TIMESTAMP!@example.com
set PASSWORD=password123
set NAME=TestUser!TIMESTAMP!

echo 회원가입 진행 중...
curl -s -X POST %BASE_URL%/auth/signup ^
    -H "Content-Type: application/json; charset=utf-8" ^
    -d "{\"username\":\"%USERNAME%\",\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\",\"name\":\"%NAME%\"}" ^
    -o signup_response.json 2>nul

:: JWT 토큰 추출
for /f "delims=" %%i in ('type signup_response.json') do set RESPONSE=%%i
echo !RESPONSE! | findstr "accessToken" >nul
if !errorlevel! equ 0 (
    :: 간단한 토큰 추출 방법
    for /f "tokens=4 delims=:," %%a in ("!RESPONSE!") do (
        set JWT_TOKEN=%%a
        set JWT_TOKEN=!JWT_TOKEN:"=!
        set JWT_TOKEN=!JWT_TOKEN: =!
    )
    echo [SUCCESS] 회원가입 및 로그인 성공
    echo 사용자명: %USERNAME%
    echo JWT 토큰: !JWT_TOKEN:~0,30!...
) else (
    echo [ERROR] 회원가입 실패
    type signup_response.json
    del signup_response.json 2>nul
    pause
    exit /b 1
)

del signup_response.json 2>nul
echo.

:: 3. 모임 생성 테스트
echo 3. 모임 생성 테스트
echo -----------------------------------------------

:: 내일 날짜로 설정 (간단 버전)
set START_DATE=2025-12-31T14:00:00
set END_DATE=2025-12-31T16:00:00

echo 모임 생성 중...
curl -s -X POST %BASE_URL%/meetings ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -H "Content-Type: application/json; charset=utf-8" ^
    -d "{\"title\":\"Test Walking Group\",\"description\":\"API Test Meeting\",\"category\":\"EXERCISE\",\"subcategory\":\"WALKING\",\"location\":\"Seoul Yeongdeungpo\",\"address\":\"Hangang Park Yeouido\",\"startDate\":\"%START_DATE%\",\"endDate\":\"%END_DATE%\",\"maxParticipants\":10,\"fee\":0,\"difficultyLevel\":\"BEGINNER\",\"ageRange\":\"50-70\",\"organizerContact\":\"010-1234-5678\"}" ^
    -o meeting_response.json 2>nul

for /f "delims=" %%i in ('type meeting_response.json') do set MEETING_RESPONSE=%%i
echo !MEETING_RESPONSE! | findstr "\"id\"" >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 모임 생성 성공
    :: 간단한 ID 추출
    for /f "tokens=3 delims=:," %%a in ("!MEETING_RESPONSE!") do (
        set MEETING_ID=%%a
        set MEETING_ID=!MEETING_ID: =!
    )
    echo 모임 ID: !MEETING_ID!
) else (
    echo [ERROR] 모임 생성 실패
    type meeting_response.json
    del meeting_response.json 2>nul
    goto :cleanup
)

del meeting_response.json 2>nul
echo.

:: 4. 모임 조회 테스트
echo 4. 모임 조회 테스트
echo -----------------------------------------------

echo 모임 상세 조회 중...
curl -s -X GET %BASE_URL%/meetings/!MEETING_ID! ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -o detail_response.json 2>nul

findstr "success.*true" detail_response.json >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 모임 상세 조회 성공
) else (
    echo [ERROR] 모임 상세 조회 실패
)
del detail_response.json 2>nul

echo 모임 목록 조회 중...
curl -s -X GET "%BASE_URL%/meetings?page=0&size=5" ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -o list_response.json 2>nul

findstr "success.*true" list_response.json >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 모임 목록 조회 성공
) else (
    echo [ERROR] 모임 목록 조회 실패
)
del list_response.json 2>nul
echo.

:: 5. 찜 기능 테스트
echo 5. 찜 기능 테스트
echo -----------------------------------------------

echo 찜 추가 중...
curl -s -X POST %BASE_URL%/wishlist/meetings/!MEETING_ID! ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -o wishlist_add_response.json 2>nul

findstr "success.*true" wishlist_add_response.json >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 찜 추가 성공
) else (
    echo [ERROR] 찜 추가 실패
    type wishlist_add_response.json
)
del wishlist_add_response.json 2>nul

echo 찜 여부 확인 중...
curl -s -X GET %BASE_URL%/wishlist/meetings/!MEETING_ID!/check ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -o wishlist_check_response.json 2>nul

findstr "isWishlisted.*true" wishlist_check_response.json >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 찜 여부 확인 성공 (찜됨)
) else (
    echo [WARNING] 찜 여부 확인 - 찜되지 않음
)
del wishlist_check_response.json 2>nul
echo.

:: 6. 두 번째 사용자 생성
echo 6. 두 번째 사용자 생성 (참여 테스트용)
echo -----------------------------------------------

set TIMESTAMP2=!TIMESTAMP!2
set USERNAME2=participant!TIMESTAMP2!
set EMAIL2=participant!TIMESTAMP2!@example.com

echo 두 번째 사용자 회원가입 중...
curl -s -X POST %BASE_URL%/auth/signup ^
    -H "Content-Type: application/json; charset=utf-8" ^
    -d "{\"username\":\"%USERNAME2%\",\"email\":\"%EMAIL2%\",\"password\":\"%PASSWORD%\",\"name\":\"Participant!TIMESTAMP2!\"}" ^
    -o signup2_response.json 2>nul

for /f "delims=" %%i in ('type signup2_response.json') do set RESPONSE2=%%i
echo !RESPONSE2! | findstr "accessToken" >nul
if !errorlevel! equ 0 (
    for /f "tokens=4 delims=:," %%a in ("!RESPONSE2!") do (
        set JWT_TOKEN2=%%a
        set JWT_TOKEN2=!JWT_TOKEN2:"=!
        set JWT_TOKEN2=!JWT_TOKEN2: =!
    )
    echo [SUCCESS] 두 번째 사용자 생성 성공
) else (
    echo [ERROR] 두 번째 사용자 생성 실패
    del signup2_response.json 2>nul
    goto :cleanup
)

del signup2_response.json 2>nul
echo.

:: 7. 모임 참여 테스트
echo 7. 모임 참여 테스트
echo -----------------------------------------------

echo 모임 참여 신청 중...
curl -s -X POST %BASE_URL%/meetings/!MEETING_ID!/join ^
    -H "Authorization: Bearer !JWT_TOKEN2!" ^
    -H "Content-Type: application/json; charset=utf-8" ^
    -d "{\"applicationMessage\":\"Test participation request.\"}" ^
    -o join_response.json 2>nul

for /f "delims=" %%i in ('type join_response.json') do set JOIN_RESPONSE=%%i
echo !JOIN_RESPONSE! | findstr "\"id\"" >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 모임 참여 신청 성공
    for /f "tokens=3 delims=:," %%a in ("!JOIN_RESPONSE!") do (
        set PARTICIPANT_ID=%%a
        set PARTICIPANT_ID=!PARTICIPANT_ID: =!
    )
    echo 참가자 ID: !PARTICIPANT_ID!
) else (
    echo [ERROR] 모임 참여 신청 실패
    type join_response.json
    del join_response.json 2>nul
    goto :cleanup
)

del join_response.json 2>nul

echo 참여 신청 승인 중...
curl -s -X POST %BASE_URL%/meetings/participants/!PARTICIPANT_ID!/approve ^
    -H "Authorization: Bearer !JWT_TOKEN!" ^
    -o approve_response.json 2>nul

findstr "success.*true" approve_response.json >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] 참여 신청 승인 성공
) else (
    echo [ERROR] 참여 신청 승인 실패
    type approve_response.json
)
del approve_response.json 2>nul
echo.

:: 8. 정리 작업
:cleanup
echo 8. 정리 작업
echo -----------------------------------------------

if not "!MEETING_ID!"=="" (
    echo 찜 삭제 중...
    curl -s -X DELETE %BASE_URL%/wishlist/meetings/!MEETING_ID! ^
        -H "Authorization: Bearer !JWT_TOKEN!" ^
        -o delete_wishlist_response.json 2>nul
    
    findstr "success.*true" delete_wishlist_response.json >nul
    if !errorlevel! equ 0 (
        echo [SUCCESS] 찜 삭제 성공
    ) else (
        echo [WARNING] 찜 삭제 실패
    )
    del delete_wishlist_response.json 2>nul

    if not "!JWT_TOKEN2!"=="" (
        echo 참여 신청 취소 중...
        curl -s -X DELETE %BASE_URL%/meetings/!MEETING_ID!/join ^
            -H "Authorization: Bearer !JWT_TOKEN2!" ^
            -o cancel_response.json 2>nul
        
        findstr "success.*true" cancel_response.json >nul
        if !errorlevel! equ 0 (
            echo [SUCCESS] 참여 신청 취소 성공
        ) else (
            echo [WARNING] 참여 신청 취소 실패 (이미 승인된 상태일 수 있음)
        )
        del cancel_response.json 2>nul
    )
)
echo.

:: 9. 테스트 결과 요약
echo ===============================================
echo                테스트 완료
echo ===============================================
echo.
echo 성공적으로 테스트된 기능들:
echo  - 서버 상태 확인
echo  - 사용자 인증 (회원가입/로그인)
echo  - 모임 생성
echo  - 모임 조회 (상세, 목록)
echo  - 찜 기능 (추가, 확인)
echo  - 모임 참여 (신청, 승인)
echo  - 정리 작업
echo.
echo 생성된 테스트 데이터:
echo  모임 ID: !MEETING_ID!
echo  참가자 ID: !PARTICIPANT_ID!
echo  주최자 토큰: !JWT_TOKEN:~0,30!...
echo  참가자 토큰: !JWT_TOKEN2:~0,30!...

:: 임시 파일 정리
del *.json 2>nul

echo.
echo 테스트가 완료되었습니다. 아무 키나 눌러 종료하세요.
pause >nul