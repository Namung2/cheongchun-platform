#!/bin/bash

# Google Cloud Run 배포 스크립트
# 청춘 백엔드 배포 자동화

echo "🚀 청춘 백엔드 Google Cloud Run 배포 시작"
echo "========================================="

# 프로젝트 정보
PROJECT_ID="cheongchun-backend-1754380666"
SERVICE_NAME="cheongchun-backend"
REGION="asia-northeast3"
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"

echo "📋 배포 정보:"
echo "- 프로젝트: ${PROJECT_ID}"
echo "- 서비스명: ${SERVICE_NAME}"
echo "- 리전: ${REGION}"
echo "- 이미지: ${IMAGE_NAME}"
echo ""

# 1. 프로젝트 설정 확인
echo "1️⃣ Google Cloud 프로젝트 설정 확인..."
gcloud config set project ${PROJECT_ID}

# 2. Docker 이미지 빌드
echo "2️⃣ Docker 이미지 빌드 중..."
docker build -t ${IMAGE_NAME} . || {
    echo "❌ Docker 빌드 실패"
    exit 1
}

# 3. Container Registry에 푸시
echo "3️⃣ Container Registry에 이미지 푸시 중..."
docker push ${IMAGE_NAME} || {
    echo "❌ 이미지 푸시 실패"
    exit 1
}

# 4. Cloud Run에 배포
echo "4️⃣ Cloud Run에 배포 중..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_NAME} \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10 \
  --port 8080 \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod \
  --quiet || {
    echo "❌ Cloud Run 배포 실패"
    exit 1
}

# 5. 배포 완료 확인
echo "5️⃣ 배포 상태 확인..."
SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format="value(status.url)")

echo ""
echo "✅ 배포 완료!"
echo "🌐 서비스 URL: ${SERVICE_URL}"
echo "🔍 Google 인증 파일: ${SERVICE_URL}/google32870450675243f1.html"
echo ""

# 6. 인증 파일 테스트
echo "6️⃣ Google 인증 파일 접근 테스트..."
echo "테스트 URL: ${SERVICE_URL}/google32870450675243f1.html"
curl -s "${SERVICE_URL}/google32870450675243f1.html" || echo "⚠️ 인증 파일 접근 실패 - 수동으로 확인하세요"

echo ""
echo "🎉 배포 및 테스트 완료!"
echo "이제 Google Search Console에서 도메인 인증을 진행하세요."