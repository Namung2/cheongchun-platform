// 📁 src/pages/Signup.js
import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Signup() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSignup = () => {
    // 비밀번호 확인
    if (formData.password !== formData.confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 여기에 실제 회원가입 API 연동
    console.log("회원가입:", formData);
    
    alert("회원가입이 완료되었습니다!");
    navigate('/login');
  };

  return (
    <div 
      style={{ 
        background: '#ffffff',
        minHeight: '100vh',
        padding: '20px 20px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        maxWidth: '430px',
        margin: '0 auto'
      }}
    >
      {/* 뒤로가기 버튼 */}
      <div style={{ width: '100%', maxWidth: '400px', marginBottom: '30px' }}>
        <button
          onClick={() => navigate('/login')}
          style={{
            background: 'none',
            border: 'none',
            fontSize: '24px',
            color: '#6C63FF',
            cursor: 'pointer',
            padding: 0
          }}
        >
          <i className="fas fa-arrow-left"></i>
        </button>
      </div>

      {/* 타이틀 */}
      <div style={{ textAlign: 'center', marginBottom: '40px' }}>
        <h1 style={{ fontSize: '2rem', color: '#6C63FF', fontWeight: 'bold', marginBottom: '10px' }}>
          회원가입
        </h1>
        <p style={{ fontSize: '15px', color: '#888', margin: 0 }}>
          하루안부와 함께 건강을 지켜요
        </p>
      </div>

      {/* 회원가입 폼 */}
      <div style={{ width: '100%', maxWidth: '400px' }}>
        <div style={{ marginBottom: '15px' }}>
          <label style={{ fontSize: '14px', color: '#666', marginBottom: '8px', display: 'block' }}>
            이름
          </label>
          <input
            type="text"
            name="name"
            placeholder="이름 입력"
            value={formData.name}
            onChange={handleChange}
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

        <div style={{ marginBottom: '15px' }}>
          <label style={{ fontSize: '14px', color: '#666', marginBottom: '8px', display: 'block' }}>
            이메일
          </label>
          <input
            type="email"
            name="email"
            placeholder="이메일 입력"
            value={formData.email}
            onChange={handleChange}
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

        <div style={{ marginBottom: '15px' }}>
          <label style={{ fontSize: '14px', color: '#666', marginBottom: '8px', display: 'block' }}>
            비밀번호
          </label>
          <input
            type="password"
            name="password"
            placeholder="비밀번호 입력 (8자 이상)"
            value={formData.password}
            onChange={handleChange}
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

        <div style={{ marginBottom: '30px' }}>
          <label style={{ fontSize: '14px', color: '#666', marginBottom: '8px', display: 'block' }}>
            비밀번호 확인
          </label>
          <input
            type="password"
            name="confirmPassword"
            placeholder="비밀번호 재입력"
            value={formData.confirmPassword}
            onChange={handleChange}
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
          onClick={handleSignup}
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
            marginBottom: '20px'
          }}
        >
          회원가입
        </button>

        <div style={{ textAlign: 'center' }}>
          <span style={{ fontSize: '14px', color: '#888' }}>
            이미 계정이 있으신가요?{' '}
            <button
              onClick={() => navigate('/login')}
              style={{
                background: 'none',
                border: 'none',
                color: '#6C63FF',
                fontWeight: 'bold',
                cursor: 'pointer',
                textDecoration: 'underline'
              }}
            >
              로그인
            </button>
          </span>
        </div>
      </div>
    </div>
  );
}

export default Signup;