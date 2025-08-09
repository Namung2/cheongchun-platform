#!/usr/bin/env python3
"""
최소한의 AI Core 서버 (기본 라이브러리만 사용)
"""

import os
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import threading
import time

# 환경변수 설정
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY', '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')
BACKEND_URL = os.getenv('SPRING_BACKEND_URL', 'https://cheongchun-backend-40635111975.asia-northeast3.run.app/api')

class AICorHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        """GET 요청 처리"""
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/health':
            self.send_health_response()
        elif parsed_path.path == '/':
            self.send_welcome_response()
        else:
            self.send_error(404, "Not Found")
    
    def do_POST(self):
        """POST 요청 처리"""
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/chat':
            self.handle_chat_request()
        else:
            self.send_error(404, "Not Found")
    
    def send_health_response(self):
        """헬스 체크 응답"""
        response = {
            "status": "healthy",
            "service": "ai-core",
            "openai_key_set": bool(OPENAI_API_KEY and OPENAI_API_KEY.startswith('sk-')),
            "backend_url": BACKEND_URL
        }
        self.send_json_response(response)
    
    def send_welcome_response(self):
        """환영 메시지"""
        response = {
            "message": "AI Core - Senior Chatbot Service",
            "version": "1.0.0",
            "status": "running"
        }
        self.send_json_response(response)
    
    def handle_chat_request(self):
        """채팅 요청 처리"""
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            request_data = json.loads(post_data.decode('utf-8'))
            
            user_message = request_data.get('message', '')
            user_id = request_data.get('user_id', 'anonymous')
            
            # 시니어 맞춤 응답 생성 (OpenAI 없이 기본 응답)
            response_message = self.generate_senior_response(user_message)
            
            response = {
                "message": response_message,
                "session_id": "test-session",
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
                "user_id": user_id
            }
            
            self.send_json_response(response)
            
        except Exception as e:
            error_response = {
                "error": "채팅 처리 중 오류가 발생했습니다.",
                "details": str(e)
            }
            self.send_json_response(error_response, status=500)
    
    def generate_senior_response(self, message):
        """시니어 맞춤 기본 응답 생성"""
        message_lower = message.lower()
        
        # 간단한 키워드 기반 응답
        if any(word in message_lower for word in ['안녕', '반갑', '좋은']):
            return "안녕하세요! 만나서 반갑습니다. 오늘 하루는 어떻게 보내고 계신가요? 😊"
        
        elif any(word in message_lower for word in ['건강', '아프', '병원', '약', '혈압', '당뇨']):
            return "건강에 관심이 많으시군요. 건강 관리는 정말 중요합니다. 정기적인 검진과 적절한 운동, 그리고 균형잡힌 식사가 도움이 될 것 같아요. 혹시 구체적으로 궁금한 점이 있으시면 말씀해 주세요."
        
        elif any(word in message_lower for word in ['외롭', '심심', '가족', '손자', '자녀']):
            return "가족과의 시간은 정말 소중하죠. 때로는 외로움을 느끼실 수도 있지만, 가족들이 항상 마음속으로는 함께하고 있다는 것을 기억해 주세요. 가족들과 연락을 자주 하시거나, 지역 커뮤니티 활동에 참여해보시는 것도 좋을 것 같아요."
        
        elif any(word in message_lower for word in ['요리', '음식', '맛있', '레시피']):
            return "요리 이야기를 좋아하시는군요! 맛있는 음식을 직접 만드는 재미는 정말 특별하죠. 어떤 음식을 즐겨 드시는지, 또는 만들어보고 싶은 요리가 있으시면 알려주세요. 간단한 레시피나 요리 팁을 알려드릴 수 있어요."
        
        elif any(word in message_lower for word in ['운동', '산책', '걷기', '체조']):
            return "운동은 건강 유지에 가장 좋은 방법 중 하나입니다! 시니어분들께는 가벼운 산책이나 실버 체조가 특히 좋아요. 무리하지 마시고 본인의 체력에 맞춰 꾸준히 하시는 것이 중요합니다. 어떤 운동을 선호하시는지 궁금하네요."
        
        else:
            return f"말씀해 주신 내용을 잘 들었습니다. 더 자세히 이야기해 주시면 더 도움이 되는 답변을 드릴 수 있을 것 같아요. 언제든지 편하게 말씀해 주세요. 항상 여러분의 이야기에 귀 기울이고 있습니다. 🤗"
    
    def send_json_response(self, data, status=200):
        """JSON 응답 전송"""
        self.send_response(status)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
        
        response_json = json.dumps(data, ensure_ascii=False, indent=2)
        self.wfile.write(response_json.encode('utf-8'))
    
    def do_OPTIONS(self):
        """CORS preflight 처리"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

def run_server():
    """서버 실행"""
    port = 8001
    server_address = ('', port)
    httpd = HTTPServer(server_address, AICorHandler)
    
    print(f"🚀 AI Core 서비스 시작!")
    print(f"📍 주소: http://localhost:{port}")
    print(f"🔑 OpenAI API: {'✅ 설정됨' if OPENAI_API_KEY else '❌ 미설정'}")
    print(f"🔗 백엔드: {BACKEND_URL}")
    print("=" * 50)
    print("📋 테스트 방법:")
    print(f"  헬스체크: curl http://localhost:{port}/health")
    print(f"  채팅테스트: curl -X POST http://localhost:{port}/chat \\")
    print(f"           -H 'Content-Type: application/json' \\")
    print(f"           -d '{{\"message\":\"안녕하세요!\",\"user_id\":\"test\"}}'")
    print("⏹️  종료: Ctrl+C")
    print("=" * 50)
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 서버를 종료합니다...")
        httpd.shutdown()

if __name__ == "__main__":
    run_server()