// backend/src/main/java/com/cheongchun/backend/controller/WebController.java
package com.cheongchun.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    /**
     * 메인 웹 페이지 - Google 로그인 테스트용
     * URL: https://cheongchun-backend-40635111975.asia-northeast3.run.app/
     */
    @GetMapping("/")
    public ResponseEntity<String> getIndexPage() {
        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>청춘 - 로그인</title>
                    <style>
                        body { 
                            font-family: 'Noto Sans KR', Arial, sans-serif; 
                            margin: 0; 
                            padding: 40px; 
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 400px;
                            width: 100%;
                        }
                        h1 { 
                            color: #333; 
                            margin-bottom: 30px;
                            font-size: 2em;
                        }
                        button { 
                            background: #4285f4; 
                            color: white; 
                            border: none; 
                            padding: 15px 30px; 
                            border-radius: 5px; 
                            cursor: pointer; 
                            font-size: 16px;
                            width: 100%;
                            margin-bottom: 15px;
                            transition: background 0.3s;
                        }
                        button:hover { 
                            background: #357ae8; 
                        }
                        .success { 
                            background: #4caf50; 
                            color: white; 
                            padding: 15px; 
                            border-radius: 5px; 
                            margin-top: 20px;
                            display: none;
                        }
                        .error { 
                            background: #f44336; 
                            color: white; 
                            padding: 15px; 
                            border-radius: 5px; 
                            margin-top: 20px;
                            display: none;
                        }
                        .user-info {
                            background: #f5f5f5;
                            padding: 15px;
                            border-radius: 5px;
                            margin-top: 20px;
                            display: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🌸 청춘</h1>
                        <p>새로운 만남의 시작</p>
                        
                        <button onclick="loginWithGoogle()" id="loginBtn">
                            🚀 Google로 로그인
                        </button>
                        
                        <button onclick="loginWithKakao()" style="background: #fee500; color: #000;">
                            💬 카카오로 로그인
                        </button>
                        
                        <button onclick="loginWithNaver()" style="background: #03c75a;">
                            📱 네이버로 로그인
                        </button>
                        
                        <div id="success" class="success"></div>
                        <div id="error" class="error"></div>
                        <div id="userInfo" class="user-info"></div>
                        
                        <button onclick="testAPI()" id="testBtn" style="background: #9c27b0; display: none;">
                            🔍 사용자 정보 조회 테스트
                        </button>
                        
                        <button onclick="logout()" id="logoutBtn" style="background: #ff5722; display: none;">
                            🚪 로그아웃
                        </button>
                    </div>

                    <script>
                        const API_BASE = window.location.origin + '/api';
                        
                        function loginWithGoogle() {
                            window.location.href = API_BASE + '/oauth2/authorization/google';
                        }
                        
                        function loginWithKakao() {
                            window.location.href = API_BASE + '/oauth2/authorization/kakao';
                        }
                        
                        function loginWithNaver() {
                            window.location.href = API_BASE + '/oauth2/authorization/naver';
                        }
                        
                        function showSuccess(message) {
                            document.getElementById('success').textContent = message;
                            document.getElementById('success').style.display = 'block';
                            document.getElementById('error').style.display = 'none';
                        }
                        
                        function showError(message) {
                            document.getElementById('error').textContent = message;
                            document.getElementById('error').style.display = 'block';
                            document.getElementById('success').style.display = 'none';
                        }
                        
                        function showUserInfo(user) {
                            const userDiv = document.getElementById('userInfo');
                            userDiv.innerHTML = `
                                <h3>로그인 성공!</h3>
                                <p><strong>이름:</strong> ${user.name}</p>
                                <p><strong>이메일:</strong> ${user.email}</p>
                                <p><strong>가입일:</strong> ${new Date(user.createdAt).toLocaleDateString()}</p>
                            `;
                            userDiv.style.display = 'block';
                            
                            document.getElementById('testBtn').style.display = 'block';
                            document.getElementById('logoutBtn').style.display = 'block';
                            document.getElementById('loginBtn').style.display = 'none';
                        }
                        
                        async function testAPI() {
                            try {
                                const token = localStorage.getItem('accessToken');
                                const response = await fetch(API_BASE + '/auth/me', {
                                    headers: {
                                        'Authorization': `Bearer ${token}`
                                    }
                                });
                                
                                if (response.ok) {
                                    const data = await response.json();
                                    showSuccess('API 테스트 성공! 사용자 정보를 가져왔습니다.');
                                    console.log('사용자 정보:', data);
                                } else {
                                    showError('API 테스트 실패: ' + response.status);
                                }
                            } catch (error) {
                                showError('API 호출 오류: ' + error.message);
                            }
                        }
                        
                        async function logout() {
                            try {
                                const token = localStorage.getItem('accessToken');
                                await fetch(API_BASE + '/auth/logout', {
                                    method: 'POST',
                                    headers: {
                                        'Authorization': `Bearer ${token}`
                                    }
                                });
                            } catch (error) {
                                console.error('로그아웃 API 호출 실패:', error);
                            } finally {
                                localStorage.removeItem('accessToken');
                                localStorage.removeItem('refreshToken');
                                location.reload();
                            }
                        }
                        
                        // 페이지 로드 시 URL에서 토큰과 사용자 정보 추출
                        window.onload = function() {
                            const urlParams = new URLSearchParams(window.location.search);
                            const token = urlParams.get('token');
                            const error = urlParams.get('error');
                            const userId = urlParams.get('userId');
                            const email = urlParams.get('email');
                            const name = urlParams.get('name');
                            
                            if (error) {
                                showError('로그인 실패: ' + decodeURIComponent(error));
                            } else if (token) {
                                localStorage.setItem('accessToken', token);
                                
                                if (userId && email && name) {
                                    showUserInfo({
                                        id: userId,
                                        email: decodeURIComponent(email),
                                        name: decodeURIComponent(name),
                                        createdAt: new Date()
                                    });
                                } else {
                                    showSuccess('로그인 성공! 토큰이 저장되었습니다.');
                                    document.getElementById('testBtn').style.display = 'block';
                                    document.getElementById('logoutBtn').style.display = 'block';
                                }
                                
                                // URL을 깔끔하게 정리
                                window.history.replaceState({}, document.title, window.location.pathname);
                            } else {
                                // 기존 토큰이 있는지 확인
                                const existingToken = localStorage.getItem('accessToken');
                                if (existingToken) {
                                    document.getElementById('testBtn').style.display = 'block';
                                    document.getElementById('logoutBtn').style.display = 'block';
                                    showSuccess('이미 로그인된 상태입니다. API 테스트를 해보세요!');
                                }
                            }
                        };
                    </script>
                </body>
                </html>
                """;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /**
     * OAuth 성공 콜백 페이지
     */
    @GetMapping("/auth/success")
    public ResponseEntity<String> getSuccessPage(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {

        String redirectUrl = String.format("/?token=%s&userId=%s&email=%s&name=%s",
                token != null ? token : "",
                userId != null ? userId : "",
                email != null ? email : "",
                name != null ? name : "");

        String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>로그인 처리 중...</title>
                    <script>
                        window.location.replace('%s');
                    </script>
                </head>
                <body>
                    <p>로그인 처리 중입니다...</p>
                </body>
                </html>
                """, redirectUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}