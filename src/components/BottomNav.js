// 📁 src/components/BottomNav.js
import { useNavigate, useLocation } from "react-router-dom";

function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <nav className="navbar navbar-light bg-white border-top fixed-bottom">
      <div className="container d-flex justify-content-around">
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/home_icon.png')}
            alt="Home Icon"
            style={{ 
              width: "22px", 
              height: "22px", 
              marginBottom: "3px",
              filter: location.pathname === '/' ? 'brightness(0) saturate(100%) invert(43%) sepia(96%) saturate(426%) hue-rotate(92deg) brightness(94%) contrast(88%)' : 'none'
            }}
          />
          <div style={{ fontSize: "12px", color: location.pathname === '/' ? '#198754' : '#212529' }}>홈</div>
        </button>
        
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/stats')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/Chat_icon.png')}
            alt="Chat Icon"
            style={{ 
              width: "22px", 
              height: "22px", 
              marginBottom: "3px",
              filter: location.pathname === '/stats' ? 'brightness(0) saturate(100%) invert(43%) sepia(96%) saturate(426%) hue-rotate(92deg) brightness(94%) contrast(88%)' : 'none'
            }}
          />
          <div style={{ fontSize: "12px", color: location.pathname === '/stats' ? '#198754' : '#212529' }}>챗봇</div>
        </button>
        
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/alerts')}
          style={{ textDecoration: 'none' }}
        >
          <i className="fas fa-bell fa-lg" style={{ color: location.pathname === '/alerts' ? '#198754' : '#212529' }}></i>
          <div style={{ fontSize: "12px", color: location.pathname === '/alerts' ? '#198754' : '#212529' }}>알림</div>
        </button>
        
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/profile')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/profile_icon.png')}
            alt="Profile Icon"
            style={{ 
              width: "22px", 
              height: "22px", 
              marginBottom: "3px",
              filter: location.pathname === '/profile' ? 'brightness(0) saturate(100%) invert(43%) sepia(96%) saturate(426%) hue-rotate(92deg) brightness(94%) contrast(88%)' : 'none'
            }}
          />
          <div style={{ fontSize: "12px", color: location.pathname === '/profile' ? '#198754' : '#212529' }}>마이페이지</div>
        </button>
      </div>
    </nav>
  );
}

export default BottomNav;