import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleLogin = () => {
    console.log("로그인:", email, password); // 여기에 실제 로그인 API 연동
    // 임시: 로그인 성공으로 처리하고 홈으로 이동
    navigate('/');
  };

  return (
    <div 
      style={{ 
        background: '#ffffff',
        minHeight: '100vh',
        padding: '40px 20px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        maxWidth: '430px',
        margin: '0 auto'
      }}
    >
      {/* 타이틀 */}
      <div style={{ textAlign: 'center', marginBottom: '50px' }}>
        <h1 style={{ fontSize: '2.5rem', color: '#6C63FF', fontWeight: 'bold', marginBottom: '10px' }}>
          안녕하세요 :)
        </h1>
        <h2 style={{ fontSize: '2rem', color: '#6C63FF', fontWeight: 'bold', marginBottom: '20px' }}>
          하루안부입니다.
        </h2>
        <p style={{ fontSize: '15px', color: '#6C63FF', margin: 0 }}>
          매일 건강을 지켜드립니다
        </p>
      </div>

      {/* 로그인 폼 */}
      <div style={{ width: '100%', maxWidth: '400px' }}>
        <div style={{ marginBottom: '15px' }}>
          <input
            type="email"
            placeholder="이메일 입력"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={{
              width: '100%',
              padding: '16px',
              borderRadius: '8px',
              border: '1px solid #e5e5e5',
              fontSize: '15px',
              background: '#f8f8f8',
              boxSizing: 'border-box'
            }}
          />
        </div>

        <div style={{ marginBottom: '25px' }}>
          <input
            type="password"
            placeholder="비밀번호 입력"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{
              width: '100%',
              padding: '16px',
              borderRadius: '8px',
              border: '1px solid #e5e5e5',
              fontSize: '15px',
              background: '#f8f8f8',
              boxSizing: 'border-box'
            }}
          />
        </div>

        <button
          onClick={handleLogin}
          style={{
            width: '100%',
            padding: '16px',
            borderRadius: '8px',
            border: 'none',
            background: '#6C63FF',
            color: 'white',
            fontWeight: '600',
            fontSize: '16px',
            cursor: 'pointer',
            marginBottom: '25px'
          }}
        >
          로그인
        </button>

        <div style={{ display: 'flex', justifyContent: 'center', gap: '20px', marginBottom: '50px' }}>
          <a 
          onClick={() => navigate('/signup')}  
          style={{ fontSize: '14px', color: '#888', textDecoration: 'none', cursor: 'pointer',fontWeight: 'bold' }}
          >
            회원가입
            </a> 
        </div>
        </div>
    </div>
  );
}

export default Login;