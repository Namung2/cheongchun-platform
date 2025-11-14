// 📁 src/pages/Profile.js
import { useNavigate } from "react-router-dom";

function Profile() {
  const navigate = useNavigate();

  const handleLogout = () => {
    if (window.confirm("로그아웃 하시겠습니까?")) {
      localStorage.removeItem('userRole');
      localStorage.removeItem('token');
      navigate('/login');
    }
  };

  return (
    <div style={{ background: '#f5f5f5', minHeight: 'calc(100vh)', paddingBottom: '80px' }}>

      {/* 프로필 섹션 */}
      <div style={{ background: 'white', marginBottom: '10px', marginTop: '10px'}}>
        <div style={{ 
          padding: '12px 16px', 
          borderBottom: '1px solid #f5f5f5',
          color: '#999',
          fontSize: '13px',
          fontWeight: '500'
        }}>
          데모 설정
        </div>
        
        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            borderBottom: '1px solid #f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <i className="fas fa-user" style={{ color: 'white', fontSize: '24px' }}></i>
            </div>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: '600', fontSize: '16px', marginBottom: '4px' }}>홍길동</div>
              <div style={{ color: '#6C63FF', fontSize: '14px' }}>프로필 보기</div>
            </div>
          </div>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>
      </div>

      {/* 일반 설정 */}
      <div style={{ background: 'white', marginBottom: '10px' }}>
        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            borderBottom: '1px solid #f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '15px' }}>알림설정</span>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>

        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            borderBottom: '1px solid #f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '15px' }}>공지사항</span>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>

        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            borderBottom: '1px solid #f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '15px' }}>비밀번호 변경</span>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>

        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '15px' }}>탈퇴요청</span>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>
      </div>

      {/* 정보 섹션 */}
      <div style={{ background: 'white', marginBottom: '10px' }}>
        <button 
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            border: 'none',
            borderBottom: '1px solid #f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
        >
          <span style={{ fontSize: '15px' }}>도움말 (FAQ)</span>
          <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
        </button>

        <button 
  onClick={handleLogout}  // ← 이거 추가!
  style={{
    width: '100%',
    padding: '16px',
    background: 'white',
    border: 'none',
    borderBottom: '1px solid #f5f5f5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    cursor: 'pointer'
  }}
>
  <span style={{ fontSize: '15px' }}>로그아웃</span>
  <i className="fas fa-chevron-right" style={{ color: '#ccc' }}></i>
</button>

        <div 
          style={{
            padding: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <span style={{ fontSize: '15px' }}>버전정보</span>
          <span style={{ fontSize: '15px', color: '#666' }}>1.0.0</span>
        </div>
      </div>
    </div>
  );
}

export default Profile;