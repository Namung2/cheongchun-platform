#!/bin/bash

# Cloud SQL 인스턴스/DB/유저 정보
INSTANCE="cheongchun-postgres"
DB="cheongchun_dev"
USER="devuser"
PASSWORD="devpass"
IP="34.64.74.16"  # Cloud SQL 공개 IP (변경 가능)
INIT_SQL="./database/init.sql"

echo "🧹 Step 1: Dropping public schema..."
psql -h $IP -U $USER -d $DB -c "DROP SCHEMA public CASCADE;" || { echo "❌ Failed to drop schema"; exit 1; }

echo "📦 Step 2: Recreating public schema..."
psql -h $IP -U $USER -d $DB -c "CREATE SCHEMA public;" || { echo "❌ Failed to create schema"; exit 1; }

echo "🗄️ Step 3: Applying init.sql..."
psql -h $IP -U $USER -d $DB -f $INIT_SQL || { echo "❌ Failed to apply init.sql"; exit 1; }

echo "✅ Database reset completed successfully!"
