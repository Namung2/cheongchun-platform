# Google Search Console 도메인 인증 설정 완료

## ✅ 완료된 작업들

### 1. Google OAuth 자격증명 확인 ✅
- **Client ID**: `40635111975-9v7492st4d2hp2m7tnn0lj1kir8mua62.apps.googleusercontent.com`
- **Client Secret**: `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`
- **Redirect URI**: `https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/login/oauth2/code/google`
- 모든 설정이 `application-prod.properties`에 정확히 반영됨

### 2. Google 인증 파일 설정 ✅
- **파일**: `google32870450675243f1.html`
- **내용**: `google-site-verification: google32870450675243f1.html`
- **위치**: `/home/namung/project/backend/src/main/resources/static/`

### 3. 백엔드 서빙 설정 ✅
다음 방법들로 인증 파일에 접근 가능하도록 설정:

#### A) 필터 방식 (GoogleVerificationFilter)
- URL: `https://cheongchun-backend-40635111975.asia-northeast3.run.app/google32870450675243f1.html`
- context-path(`/api`) 우회하여 루트 경로에서 직접 접근

#### B) 컨트롤러 방식 (StaticController)
- URL: `https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/google32870450675243f1.html`

#### C) Resource Handler 방식 (WebConfig)
- 정적 리소스로 자동 서빙

## 🚀 배포 및 테스트 절차

### 1. 백엔드 배포
```bash
cd /home/namung/project/backend
./gradlew build -x test
# Google Cloud Run에 배포
```

### 2. 인증 파일 접근 테스트
배포 후 다음 URL에서 파일이 정상적으로 제공되는지 확인:
```
https://cheongchun-backend-40635111975.asia-northeast3.run.app/google32870450675243f1.html
```

예상 응답:
```
google-site-verification: google32870450675243f1.html
```

### 3. Google Search Console에서 인증
1. [Google Search Console](https://search.google.com/search-console) 접속
2. 속성 추가 → URL 접두어: `https://cheongchun-backend-40635111975.asia-northeast3.run.app`
3. HTML 파일 업로드 방법 선택
4. 파일이 이미 업로드되었다고 표시되면 **확인** 클릭

### 4. OAuth 동의 화면 업데이트
인증 완료 후 Google Cloud Console에서:
1. **OAuth 동의 화면** → **앱 도메인** 섹션
2. **승인된 도메인**: `asia-northeast3.run.app` 추가
3. **애플리케이션 홈페이지**: `https://cheongchun-backend-40635111975.asia-northeast3.run.app`

## 🔍 트러블슈팅

### 파일에 접근할 수 없는 경우
1. **서버 로그 확인**: 필터와 컨트롤러가 제대로 동작하는지
2. **CORS 설정**: 브라우저에서 직접 접근 시 CORS 문제 없는지
3. **SSL 인증서**: HTTPS 연결이 정상인지

### Google Search Console 인증 실패 시
1. **파일 내용 확인**: 정확히 `google-site-verification: google32870450675243f1.html`인지
2. **URL 접근**: 브라우저에서 직접 파일 URL 접근해보기
3. **캐시 클리어**: 브라우저 캐시 삭제 후 재시도

## 📝 다음 단계
1. 백엔드를 Google Cloud Run에 배포
2. 인증 파일 URL 접근 테스트
3. Google Search Console에서 도메인 인증
4. Google OAuth 로그인 테스트

모든 설정이 완료되었으므로 이제 배포 후 테스트를 진행하시면 됩니다!