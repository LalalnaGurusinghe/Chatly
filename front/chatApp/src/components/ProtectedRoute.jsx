import { Children } from "react";
import { authService } from "../service/authServices";

const ProtectedRoute = ({Children})=>{
    const isAuthenticated = authService.isAuthenticated();

    if(!isAuthenticated){
        return <Navigate to="/login" replace/>
    }
    return Children;
}

export default ProtectedRoute;