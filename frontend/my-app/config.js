// 환경별 설정 파일
const Config = {
  // API 서버 설정
  API: {
    BASE_URL: 'https://cheongchun-backend-40635111975.asia-northeast3.run.app',
    TIMEOUT: 10000,
  },
  
  // OAuth2 설정
  OAUTH: {
    GOOGLE_URL: 'https://cheongchun-backend-40635111975.asia-northeast3.run.app/oauth2/authorization/google',
    KAKAO_URL: 'https://cheongchun-backend-40635111975.asia-northeast3.run.app/oauth2/authorization/kakao',
    NAVER_URL: 'https://cheongchun-backend-40635111975.asia-northeast3.run.app/oauth2/authorization/naver',
  },
  
  // 딥링크 설정
  DEEP_LINK: {
    SCHEME: 'myapp',
    AUTH_SUCCESS: 'myapp://auth-success',
    AUTH_ERROR: 'myapp://auth-error',
  },
  
  // 앱 설정
  APP: {
    NAME: '청춘',
    VERSION: '1.0.0',
  },
};

export default Config;