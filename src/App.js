// 📁 src/App.js
import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "@fortawesome/fontawesome-free/css/all.min.css";
import "./styles.css";

// Components
import SplashScreen from "./components/SplashScreen";
import MainLayout from "./components/MainLayout";

// Pages
import Dashboard from "./pages/Dashboard";
import Stats from "./pages/Chat";
import Alerts from "./pages/Alerts";
import Profile from "./pages/Profile";

function App() {
  const [loading, setLoading] = useState(true);
  const [fadeOut, setFadeOut] = useState(false);

  useEffect(() => {
    const fadeTimer = setTimeout(() => {
      setFadeOut(true);
    }, 500);

    const removeTimer = setTimeout(() => {
      setLoading(false);
    }, 800);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(removeTimer);
    };
  }, []);

  return (
    <Router>
      <div style={{ 
        maxWidth: '430px', 
        margin: '0 auto',
        position: 'relative',
        boxShadow: '0 0 20px rgba(0,0,0,0.1)',
        minHeight: '100vh'
      }}>
        {loading && <SplashScreen fadeOut={fadeOut} />}
        
        <MainLayout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/stats" element={<Stats />} />
            <Route path="/alerts" element={<Alerts />} />
            <Route path="/profile" element={<Profile />} />
          </Routes>
        </MainLayout>
      </div>
    </Router>
  );
}

export default App;