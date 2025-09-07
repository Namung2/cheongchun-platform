#!/bin/bash

echo "🧪 Step 1: Running tests..."
./gradlew test || { echo "❌ Tests failed"; exit 1; }

echo "🐳 Step 2: Building & pushing Docker image..."
docker buildx build --platform linux/amd64 \
  -t asia-northeast3-docker.pkg.dev/cheongchun-backend-1754380666/cloud-run-source-deploy/backend:latest \
  --push . || { echo "❌ Docker build failed"; exit 1; }

echo "🚀 Step 3: Deploying to Cloud Run..."
gcloud run deploy cheongchun-backend \
  --image=asia-northeast3-docker.pkg.dev/cheongchun-backend-1754380666/cloud-run-source-deploy/backend:latest \
  --platform=managed \
  --region=asia-northeast3 \
  --allow-unauthenticated \
  --add-cloudsql-instances=cheongchun-backend-1754380666:asia-northeast3:cheongchun-postgres \
  --set-env-vars=SPRING_PROFILES_ACTIVE=prod || { echo "❌ Deploy failed"; exit 1; }

echo "✅ Deployment completed successfully!"
