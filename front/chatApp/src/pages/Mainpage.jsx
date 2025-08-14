import { useNavigate } from "react-router-dom";
import "../styles/Auth.css";

const MainPage = () => {
  const navigate = useNavigate();

  const handleGettingStarted = () => {
    navigate("/signup");
  };

  const handleLogin = () => {
    navigate("/login");
  };

  return (
    <div className="mainpage-container">
      <h1 className="mainpage-title">
        Welcome to the Real-time Chat Application
      </h1>
      <p style={{ color: 'white', fontSize: '1.2rem', marginBottom: '2rem', opacity: 0.9 }}>
        Connect with friends and family in real-time conversations
      </p>
      <div style={{ 
        background: 'rgba(255, 255, 255, 0.1)', 
        padding: '1rem', 
        borderRadius: '10px', 
        marginBottom: '2rem',
        border: '1px solid rgba(255, 255, 255, 0.2)'
      }}>
        <p style={{ color: 'white', fontSize: '0.9rem', margin: 0, opacity: 0.8 }}>
          💡 Make sure your backend server is running on port 8080 for full functionality
        </p>
      </div>
      <div className="mainpage-button" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', justifyContent: 'center' }}>
        <button className="btn btn-primary" onClick={handleGettingStarted}>
          Getting Started
        </button>
        <button className="btn" style={{ background: 'rgba(255, 255, 255, 0.2)', color: 'white', border: '2px solid white' }} onClick={handleLogin}>
          Login
        </button>
      </div>
    </div>
  );
};

export default MainPage;
