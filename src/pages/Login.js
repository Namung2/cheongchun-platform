import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {
  const [email] = useState("");
  const [password] = useState("");
  const navigate = useNavigate();

  const handleLogin = () => {
    console.log("로그인:", email, password); // 여기에 실제 로그인 API 연동 아마 이거 필요없고 소셜전환 코드 넣어야할 듯 
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

        {/* SNS 로그인 */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: '15px', flexWrap: 'wrap' }}>
          <button 
            style={{ 
              width: '100%',
              padding: '16px',
              borderRadius: '8px',
              border: 'none',
              background: '#FEE500',
              color: '#3C1E1E',
              fontSize: '15px',
              fontWeight: '500',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}
          >
            <i className="fas fa-comment" style={{ fontSize: '20px' }}></i>
            카카오로 계속하기 
          </button>

          <button 
            style={{ 
              width: '100%',
              padding: '12px',
              borderRadius: '8px',
              border: 'none',
              background: '#03C75A',
              color: 'white',
              fontSize: '15px',
              fontWeight: '500',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}
          >
            <span style={{ fontSize: '20px', fontWeight: 'bold' }}>N</span>
            네이버로 계속하기 
          </button>

          <button 
            style={{ 
              width: '100%',
              padding: '16px',
              borderRadius: '8px',
              border: '1px solid #000000',
              background: '#ffffff',
              color: '#000000',
              fontSize: '15px',
              fontWeight: '500',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}
          >
            <i className="fab fa-google" style={{ fontSize: '20px' }}></i>
            Google로 계속하기 
          </button>

          <button 
            onClick={() => navigate('/email-login')}
            style={{ 
              width: '100%',
              padding: '16px',
              borderRadius: '8px',
              border: 'none',
              background: '#6C63FF',
              color: 'white',
              fontSize: '15px',
              fontWeight: '500',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}
          >
            <i className="fas fa-envelope" style={{ fontSize: '20px' }}></i>
            이메일 로그인 · 회원가입
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login;