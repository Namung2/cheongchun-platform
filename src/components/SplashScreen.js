// 📁 src/components/SplashScreen.js
function SplashScreen({ fadeOut }) {
  return (
    <div 
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: '100%',
        height: '100%',
        background: '#1fe56bff',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 99999,
        opacity: fadeOut ? 0 : 1,
        transition: 'opacity 0.5s ease-out'
      }}
    >
      <div className="text-center px-4">
        <h1 className="text-white" style={{ 
          fontSize: '3rem', 
          fontWeight: '900',
          letterSpacing: '2px'
        }}>
          하루안부
        </h1>
      </div>
    </div>
  );
}

export default SplashScreen;