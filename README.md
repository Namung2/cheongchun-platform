# 🌿 청춘 플랫폼 (Cheongchun Platform)

시니어(만 65세 이상)를 위한 **AI 기반 모임 매칭 및 커뮤니티 플랫폼**

---

## 📋 목차
1. [프로젝트 소개](#-프로젝트-소개)
2. [주요 기능](#-주요-기능)
3. [기술 스택](#-기술-스택)
4. [시스템 아키텍처](#-시스템-아키텍처)
5. [프로젝트 구조](#-프로젝트-구조)
6. [API 문서](#-api-문서)
7. [라이선스](#-라이선스)
8. [팀 & 문의](#-팀--문의)

---

## 🎯 프로젝트 소개

**청춘 플랫폼**은 시니어들의 활기찬 노후 생활을 지원하기 위한 종합 소셜 플랫폼이다.  
AI 기반 개인화 추천, 실시간 채팅, 직관적인 모임 관리 기능을 제공한다.

### 핵심 가치
- 🤖 **AI 맞춤 추천**: GPT-4 기반 시니어 친화적 AI 도우미  
- 🎯 **개인화 서비스**: 관심사 및 건강 프로필 기반 추천  
- 💬 **실시간 소통**: WebSocket 기반 실시간 채팅  
- 🔐 **간편 로그인**: Google, Naver, Kakao 소셜 로그인 지원  

---

## ✨ 주요 기능

### 1️⃣ AI 챗봇 서비스
- 시니어 맞춤 대화 (존댓말, 쉬운 설명)
- WebSocket 기반 실시간 응답
- 감정 및 건강 관심사 분석
- AI 개인화 프로필 기반 맞춤형 답변

### 2️⃣ 모임 관리
- 모임 생성/검색 (카테고리별)
- 신청, 승인, 거절 관리
- 자동 승인 (선착순)
- 찜 및 알림 기능

### 3️⃣ 인증 및 보안
- OAuth2 (Google, Naver, Kakao)
- 이메일 인증
- JWT 및 리프레시 토큰 기반 인증

### 4️⃣ 사용자 프로필
- AI 학습 기반 프로필
- 관심사 관리
- 활동 통계 (참여, 대화, 분석)

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.3  
- **Language**: Java 17  
- **Security**: Spring Security + JWT  
- **Database**: PostgreSQL (pgvector)  
- **Cache**: Redis  
- **Build Tool**: Gradle  

### AI Services
- **Framework**: FastAPI  
- **Language**: Python 3.11+  
- **AI Model**: OpenAI GPT-4  
- **Communication**: WebSocket + REST API  

### Infrastructure
- **Cloud**: Google Cloud Platform (Cloud Run, Cloud SQL)  
- **Container**: Docker  
- **CI/CD**: GitHub Actions  

---

## 🏗 시스템 아키텍처

```
┌─────────────────┐
│  React Native   │
│   Frontend      │
└────────┬────────┘
         │ HTTP/WebSocket
         ▼
┌─────────────────┐     ┌──────────────┐
│  Spring Boot    │────▶│  PostgreSQL  │
│    Backend      │     │   (pgvector) │
│   :8080         │◀────│              │
└────────┬────────┘     └──────────────┘
         │ HTTP
         ▼
┌─────────────────┐     ┌──────────────┐
│   FastAPI       │────▶│  OpenAI API  │
│   AI-Core       │     │   GPT-4      │
│   :8001         │     └──────────────┘
└─────────────────┘
```

---

## 🚀 시작하기

### 필수 요구사항
- Java 17+
- Python 3.11+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis


## 📁 프로젝트 구조

```
cheongchun-platform/
├── backend/
│   ├── src/main/java/com/cheongchun/backend/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── security/
│   │   └── util/
│   ├── build.gradle
│   └── Dockerfile
│
├── ai-services/
│   ├── ai-core/
│   │   ├── services/
│   │   │   ├── openai_service.py
│   │   │   ├── backend_service.py
│   │   │   └── conversation_analyzer.py
│   │   ├── main.py
│   │   ├── run.py
│   │   └── README.md
│   └── requirements.txt
│
├── database/
│   └── init.sql
│
├── scripts/
│   ├── check-db.sh
│   ├── cleanup-docker.sh
│   └── deploy.sh
│
├── docker-compose.dev.yml
└── README.md
```

---

## 📚 API 문서 - Docs

#### 성공 응답
```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2025-11-09T10:30:00"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": "상세 정보"
  },
  "timestamp": "2025-11-09T10:30:00"
}
```

---

## 🤝 기여하기
```bash
git checkout -b feature/AmazingFeature
git commit -m "Add some AmazingFeature"
git push origin feature/AmazingFeature
```
Pull Request를 생성한다.

---

## 📝 라이선스
이 프로젝트는 **MIT License** 하에 배포된다.

---

## 👥 팀 & 문의
- **Backend**: Spring Boot + PostgreSQL  
- **AI Services**: FastAPI + OpenAI GPT-4  
- **Frontend**: React Native  

📎 **프로젝트 링크**: [GitHub Repository](https://github.com/your-repo/cheongchun-platform)  

---

> 🌸 청춘 플랫폼으로 시니어들의 활기찬 제2의 인생을 응원합니다.
