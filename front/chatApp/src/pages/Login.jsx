import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "../service/authServices";

const Login = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setMessage("");
    setIsLoading(true);

    try {
      const result = await authService.Login(username, password);
      if (result.success) {
        setMessage("Login successfully.");
        setTimeout(() => {
          navigate("/chatarea");
        }, 2000);
      }
    } catch (error) {
      setMessage(error.message || "Login failed Please try again");
      console.error("Login filed", error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <div className="login-header">
          <h1>Login</h1>
          <p>Create an account to start chatting</p>
        </div>

        <form onSubmit={handleLogin} className="Login-form">
          <input
            type="text"
            placeholder="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="username-input"
            maxLength={20}
            required
            disabled={isLoading}
          />

          <input
            type="password"
            placeholder="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="username-input"
            maxLength={20}
            required
            disabled={isLoading}
          />

          <button
            type="submit"
            disabled={
              !username.trim() ||  !password.trim() || isLoading
            }
            className="join-btn"
          >
            {isLoading ? "Logging In..." : "Login"}
          </button>

          {message && (
            <p
              className="auth-message"
              style={{
                color: message.includes("successfully") ? "#4CaF50" : "#ff6b6b",
              }}
            ></p>
          )}
        </form>
      </div>
    </div>
  );
};

export default Login;
