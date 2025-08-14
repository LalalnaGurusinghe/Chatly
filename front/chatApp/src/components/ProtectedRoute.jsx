import { Navigate } from "react-router-dom";
import { authService } from "../service/authServices";

const ProtectedRoute = ({ children }) => {
    const authStatus = authService.checkAuthStatus();

    if (!authStatus.isAuthenticated) {
        return <Navigate to="/login" replace />
    }
    return children;
}

export default ProtectedRoute;