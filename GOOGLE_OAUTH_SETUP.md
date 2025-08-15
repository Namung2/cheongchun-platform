# Google OAuth 설정 확인 가이드

## 1. Google Cloud Console 설정 확인

### 프로젝트 정보
- **Client ID**: `40635111975-9v7492st4d2hp2m7tnn0lj1kir8mua62.apps.googleusercontent.com`
- **프로젝트**: `cheongchun-backend-1754380666`

### 필수 확인 사항

#### 1) OAuth 동의 화면 설정
- **애플리케이션 이름**: 청춘 또는 Cheongchun
- **사용자 지원 이메일**: 설정되어 있는지 확인
- **앱 도메인**: `https://cheongchun-backend-40635111975.asia-northeast3.run.app`
- **승인된 도메인**: `asia-northeast3.run.app` 추가
- **개발자 연락처 정보**: 이메일 주소 설정

#### 2) OAuth 2.0 클라이언트 ID 설정
다음 리다이렉트 URI가 **반드시** 등록되어 있어야 합니다:

```
https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/login/oauth2/code/google
```

#### 3) 범위(Scope) 설정
- `openid`
- `https://www.googleapis.com/auth/userinfo.profile`
- `https://www.googleapis.com/auth/userinfo.email`

## 2. 테스트 사용자 추가 (필요시)
앱이 아직 검토 대기중인 경우, 테스트 사용자를 추가해야 합니다:
- OAuth 동의 화면 > 테스트 사용자
- 테스트할 Google 계정 이메일 추가

## 3. 앱 상태 확인
- **게시 상태**: 프로덕션 또는 테스트 중
- **확인 상태**: 확인된 앱인지 확인

## 4. 모바일 앱 설정 (선택사항)
Android 앱을 추가하려면:
- **패키지 이름**: `com.yourcompany.myapp` (expo 빌드 시 확인)
- **SHA-1 인증서 지문**: 개발/프로덕션 키 추가

## 5. 디버깅을 위한 로그 확인
Google Cloud Console > API 및 서비스 > OAuth 2.0 > 로그에서 실패 원인 확인 가능

## 문제 해결 체크리스트
- [ ] 리다이렉트 URI가 정확히 일치하는가?
- [ ] 앱이 게시 상태인가?
- [ ] 테스트 사용자가 추가되었는가? (테스트 모드인 경우)
- [ ] 클라이언트 ID와 시크릿이 올바른가?
- [ ] 도메인이 승인된 도메인에 추가되었는가?