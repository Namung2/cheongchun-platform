# 소셜 로그인 테스트 가이드

## 1. 소셜 로그인 URL

### Google 로그인
```
http://localhost:8080/oauth2/authorization/google
```

### Naver 로그인
```
http://localhost:8080/oauth2/authorization/naver
```

### Kakao 로그인
```
http://localhost:8080/oauth2/authorization/kakao
```

## 2. 소셜 로그인 플로우

1. **로그인 시작**: 위 URL 중 하나를 브라우저에서 접속
2. **OAuth 인증**: 해당 소셜 플랫폼의 로그인 페이지로 리다이렉트
3. **권한 승인**: 사용자가 앱에 권한을 부여
4. **콜백 처리**: 앱이 사용자 정보를 받아 처리
5. **JWT 토큰 발급**: 성공 시 JWT 토큰과 함께 프론트엔드로 리다이렉트

## 3. 테스트 환경 설정

### 환경 변수 설정 (실제 값으로 변경 필요)

```bash
# Google OAuth2
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"

# Naver OAuth2
export NAVER_CLIENT_ID="your-naver-client-id"
export NAVER_CLIENT_SECRET="your-naver-client-secret"

# Kakao OAuth2
export KAKAO_CLIENT_ID="your-kakao-client-id"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
```

### 콜백 URL 설정

각 플랫폼에서 다음 콜백 URL을 등록해야 합니다
"http://localhost:8080/api/login/oauth2/code/google
http://localhost:8080/api/login/oauth2/code/kakao
http://localhost:8080/api/login/oauth2/code/naver"


## 4. 테스트 방법

### 4.1 기본 테스트
1. 애플리케이션 실행
2. 브라우저에서 소셜 로그인 URL 접속
3. 소셜 플랫폼 로그인 진행
4. 성공 시 `http://localhost:3000/auth/success?token=JWT_TOKEN`으로 리다이렉트

### 4.2 API 테스트
소셜 로그인 후 발급받은 JWT 토큰으로 API 테스트:

```bash
# 받은 JWT 토큰 사용
JWT_TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# 현재 사용자 정보 조회
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## 5. 트러블슈팅

### 5.1 일반적인 문제
- **403 Forbidden**: 클라이언트 ID/Secret이 잘못되었거나 콜백 URL이 등록되지 않음
- **400 Bad Request**: 잘못된 스코프 또는 권한 요청
- **Redirect URI mismatch**: 등록된 콜백 URL과 실제 콜백 URL이 다름

### 5.2 로그 확인
```bash
# 애플리케이션 로그 확인
tail -f logs/spring.log

# 또는 콘솔 로그 확인
```

### 5.3 데이터베이스 확인
```sql
-- 생성된 사용자 확인
SELECT * FROM users WHERE provider_type != 'LOCAL';

-- 소셜 계정 정보 확인
SELECT * FROM social_accounts;
```

## 6. 개발자 콘솔 설정

### Google Cloud Console
1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. 프로젝트 생성 또는 선택
3. "APIs & Services" > "Credentials" 이동
4. "Create Credentials" > "OAuth 2.0 Client IDs" 선택
5. 리다이렉트 URI 설정

### Naver Developers
1. [Naver Developers](https://developers.naver.com) 접속
2. 애플리케이션 등록
3. "로그인 오픈API" 서비스 추가
4. 리다이렉트 URI 설정

### Kakao Developers
1. [Kakao Developers](https://developers.kakao.com) 접속
2. 애플리케이션 생성
3. "카카오 로그인" 활성화
4. 리다이렉트 URI 설정
5. 동의항목 설정 (프로필 정보, 카카오계정 이메일)