-- 청춘장터 데이터베이스 초기화 스크립트 (username 필드 포함)
-- 파일 위치: database/init.sql

-- 확장 기능 설치
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 타임존 설정
SET timezone = 'Asia/Seoul';

-- 사용자 기본 정보 테이블 (username 필드 추가)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,    -- 사용자가 직접 생성하는 아이디
    email VARCHAR(255) UNIQUE NOT NULL,      -- 이메일 (별도 관리)
    password VARCHAR(255), -- 소셜 로그인 시 NULL
    name VARCHAR(100) NOT NULL,
    age INTEGER,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    location VARCHAR(100),
    detailed_address VARCHAR(200),
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    
    -- 추가 프로필 정보
    past_job VARCHAR(100),
    is_retired BOOLEAN DEFAULT FALSE,
    health_status VARCHAR(20) DEFAULT 'GOOD' CHECK (health_status IN ('GOOD', 'FAIR', 'LIMITED')),
    activity_level VARCHAR(20) DEFAULT 'MODERATE' CHECK (activity_level IN ('HIGH', 'MODERATE', 'LOW')),
    preferred_meeting_size VARCHAR(20) DEFAULT 'MEDIUM' CHECK (preferred_meeting_size IN ('SMALL', 'MEDIUM', 'LARGE')),
    preferred_activity_type VARCHAR(20) DEFAULT 'BOTH' CHECK (preferred_activity_type IN ('INDOOR', 'OUTDOOR', 'BOTH')),
    available_weekdays TEXT, -- JSON 형태: ["MON", "TUE", "WED"]
    available_times TEXT, -- JSON 형태: ["MORNING", "AFTERNOON", "EVENING"]
    
    -- AI 추천용 벡터 (나중에 사용)
    profile_vector vector(384), -- sentence-transformers 기본 차원
    
    -- 계정 상태 및 Spring Security 필드
    role VARCHAR(20) NOt NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    provider_type VARCHAR(20) DEFAULT 'LOCAL' CHECK (provider_type IN ('LOCAL', 'GOOGLE', 'NAVER', 'KAKAO')),
    provider_id VARCHAR(100),
    account_status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 소셜 로그인 정보 테이블
CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE')),
    provider_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(255),
    provider_name VARCHAR(100),
    profile_image_url VARCHAR(500),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(provider, provider_id)
);

-- 사용자 관심사 테이블
CREATE TABLE IF NOT EXISTS user_interests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL CHECK (category IN ('HOBBY', 'EXERCISE', 'CULTURE', 'EDUCATION', 'TALK', 'VOLUNTEER')),
    interest VARCHAR(100) NOT NULL,
    priority INTEGER DEFAULT 1 CHECK (priority > 0 AND priority <= 5),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 모임 기본 정보 테이블 (vector 컬럼 포함)
CREATE TABLE IF NOT EXISTS meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL CHECK (category IN ('HOBBY', 'EXERCISE', 'CULTURE', 'EDUCATION', 'TALK', 'VOLUNTEER')),
    subcategory VARCHAR(50),
    location VARCHAR(200),
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- AI 추천용 벡터
    description_vector vector(384), -- 모임 설명 임베딩
    
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    max_participants INTEGER CHECK (max_participants > 0),
    current_participants INTEGER DEFAULT 0 CHECK (current_participants >= 0),
    fee INTEGER DEFAULT 0 CHECK (fee >= 0),
    difficulty_level VARCHAR(20) DEFAULT 'BEGINNER' CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    age_range VARCHAR(50),
    meeting_type VARCHAR(20) DEFAULT 'USER_CREATED' CHECK (meeting_type IN ('USER_CREATED', 'CRAWLED')),
    source VARCHAR(100),
    
    -- 추가 정보
    organizer_contact VARCHAR(100),
    preparation_needed TEXT,
    meeting_rules TEXT,
    
    -- 통계 정보
    view_count INTEGER DEFAULT 0 CHECK (view_count >= 0),
    daily_view_count INTEGER DEFAULT 0 CHECK (daily_view_count >= 0),
    wishlist_count INTEGER DEFAULT 0 CHECK (wishlist_count >= 0),
    share_count INTEGER DEFAULT 0 CHECK (share_count >= 0),
    
    status VARCHAR(20) DEFAULT 'RECRUITING' CHECK (status IN ('RECRUITING', 'FULL', 'CLOSED', 'CANCELLED')),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 모임 참가자 테이블
CREATE TABLE IF NOT EXISTS meeting_participants (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    application_message TEXT,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(meeting_id, user_id)
);

-- 찜 기능 테이블
CREATE TABLE IF NOT EXISTS user_wishlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    meeting_id BIGINT REFERENCES meetings(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, meeting_id)
);

-- 채팅방 테이블
CREATE TABLE IF NOT EXISTS chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meetings(id) ON DELETE CASCADE,
    name VARCHAR(200),
    room_type VARCHAR(20) DEFAULT 'GROUP' CHECK (room_type IN ('GROUP', 'PRIVATE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 채팅 메시지 테이블
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    content TEXT,
    message_type VARCHAR(20) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),
    file_url VARCHAR(500),
    reply_to_message_id BIGINT REFERENCES chat_messages(id) ON DELETE SET NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 알림 설정 테이블
CREATE TABLE IF NOT EXISTS notification_settings (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE PRIMARY KEY,
    meeting_application BOOLEAN DEFAULT TRUE,
    meeting_schedule_change BOOLEAN DEFAULT TRUE,
    chat_message BOOLEAN DEFAULT TRUE,
    recommendation BOOLEAN DEFAULT TRUE,
    wishlist_update BOOLEAN DEFAULT TRUE,
    marketing BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 차단 사용자 테이블
CREATE TABLE IF NOT EXISTS blocked_users (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_location ON users(location);
CREATE INDEX IF NOT EXISTS idx_meetings_category ON meetings(category);
CREATE INDEX IF NOT EXISTS idx_meetings_location ON meetings(location);
CREATE INDEX IF NOT EXISTS idx_meetings_start_date ON meetings(start_date);
CREATE INDEX IF NOT EXISTS idx_meetings_status ON meetings(status);
CREATE INDEX IF NOT EXISTS idx_user_interests_category ON user_interests(category);
CREATE INDEX IF NOT EXISTS idx_user_wishlists_user_id ON user_wishlists(user_id);
CREATE INDEX IF NOT EXISTS idx_meeting_participants_meeting_id ON meeting_participants(meeting_id);
CREATE INDEX IF NOT EXISTS idx_meeting_participants_user_id ON meeting_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_room_id ON chat_messages(room_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at);

-- Vector 관련 인덱스 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_meetings_description_vector ON meetings USING ivfflat (description_vector vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_users_profile_vector ON users USING ivfflat (profile_vector vector_cosine_ops) WITH (lists = 100);

-- 기본 테스트 데이터 삽입 (username 포함)
INSERT INTO users (username, email, name, age, gender, location, past_job, is_retired, role, provider_type) VALUES
    ('cheolsu', 'test@cheongchun.com', '김철수', 55, 'MALE', '서울시 강남구', '회사원', TRUE, 'USER', 'LOCAL'),
    ('admin', 'admin@cheongchun.com', '관리자', 50, 'FEMALE', '서울시 서초구', '교사', TRUE, 'ADMIN', 'LOCAL'),
    ('younghee', 'user1@example.com', '박영희', 58, 'FEMALE', '서울시 송파구', '간호사', TRUE, 'USER', 'LOCAL'),
    ('minsu', 'user2@example.com', '이민수', 62, 'MALE', '서울시 영등포구', '공무원', TRUE, 'USER', 'LOCAL')
ON CONFLICT (username) DO NOTHING;

-- 관심사 데이터 삽입
INSERT INTO user_interests (user_id, category, interest, priority) VALUES
    (1, 'EXERCISE', '산책', 1),
    (1, 'HOBBY', '독서', 2),
    (2, 'CULTURE', '영화감상', 1),
    (2, 'EDUCATION', '컴퓨터', 2),
    (3, 'EXERCISE', '요가', 1),
    (3, 'HOBBY', '요리', 2),
    (4, 'VOLUNTEER', '봉사활동', 1)
ON CONFLICT DO NOTHING;

-- 모임 데이터 삽입
INSERT INTO meetings (title, description, category, subcategory, location, address, start_date, end_date, max_participants, fee, difficulty_level, age_range, source, organizer_contact, created_by) VALUES
    ('한강 산책 모임', '한강에서 함께 산책하며 건강도 챙기고 친목도 다져요', 'EXERCISE', 'WALKING', '서울시 영등포구', '한강공원 여의도지구', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 2 hours', 15, 0, 'BEGINNER', '50-70세', '사용자 생성', '010-1234-5678', 1),
    ('독서 토론 모임', '이달의 책을 읽고 함께 토론해요', 'HOBBY', 'READING', '서울시 강남구', '강남구립도서관', NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 2 hours', 10, 5000, 'BEGINNER', '50-65세', '사용자 생성', '010-2345-6789', 2),
    ('요리 배우기', '전통 한식 만들기를 함께 배워요', 'HOBBY', 'COOKING', '서울시 송파구', '송파구 문화센터', NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days 3 hours', 12, 15000, 'INTERMEDIATE', '55-70세', '사용자 생성', '010-3456-7890', 3),
    ('영화 감상 모임', '클래식 영화를 함께 보고 이야기해요', 'CULTURE', 'MOVIE', '서울시 서초구', '서초구 영화관', NOW() + INTERVAL '4 days', NOW() + INTERVAL '4 days 2 hours', 20, 8000, 'BEGINNER', '50-75세', '사용자 생성', '010-4567-8901', 4)
ON CONFLICT DO NOTHING;

-- 찜 데이터 삽입
INSERT INTO user_wishlists (user_id, meeting_id) VALUES
    (1, 2), (1, 3), (2, 1), (2, 4), (3, 1), (3, 4), (4, 2), (4, 3)
ON CONFLICT DO NOTHING;

-- 알림 설정 기본값 삽입
INSERT INTO notification_settings (user_id) VALUES (1), (2), (3), (4)
ON CONFLICT DO NOTHING;
