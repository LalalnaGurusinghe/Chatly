import axios from "axios";
import Signup from "../pages/Signup";

const API_URL = "http://localhost:8080";

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

//Response interceptor for global error handeling

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          authService.logout();
          window.location.href = "/login";
          break;
        case 403:
          console.error(
            "Forbidden - you do not have permission to access this resource."
          );
          break;
        case 404:
          console.error("Resource not found - check the URL and try again.");
          break;
        default:
          console.error("An unexpected error occurred:", error.response.data);
      }
    } else if (error.request) {
      console.error("No response received from API:", error.request);
    } else {
      console.error("Error setting up API request:", error.message);
    }
    return Promise.reject(error);
  }
);

const generateUserColor = () => {
  const colors = ["#FF5733", "#33FF57", "#3357FF", "#F3FF33", "#FF33F6" ,"#BB8FCE","#DDA00D"];

  return colors[Math.floor(Math.random() * colors.length)];
  
};

export const authService = {

  login:async(username , password )=>{
    try {
      const response = await api.post("/auth/login", { username, password });

      //After successfully login
      const userColor = generateUserColor();
      
      const userData = {
        ...response.data,
        color:userColor,
        loginTime : new Date().toISOString()
      };

      localStorage.setItem("currentUser", JSON.stringify(userData));
      localStorage.setItem('user',JSON.stringify(response.data));
      

      return{
        success: true,
        user: userData
      };

      
    } catch (error) {
      console.error("Error logging in:", error);
      throw error;
    }
  },


  Signup: async(username, email, password) => {
    try{

        const response = await api.post("/auth/signup", {
          username,
          email,
          password,
        });

        return{
            success: true,
            user: response.data
        };

    }
    catch (error) {
      console.error("Error signing up:", error);
      throw error;
    }
  },

  logout: async () => {
    try {
      await api.post("/auth/logout");
      localStorage.removeItem("currentUser");
      localStorage.removeItem("user");
      return { success: true };
    } catch (error) {
      console.error("Error logging out:", error);
      throw error;
    }
  },


  fetchCurrentUser: async()=>{
    try{
      const response = await api.get("/auth/current-user");
      localStorage.setItem("currentUser", JSON.stringify(response.data));
      return {
        success: true,
        user: response.data
      };
    }
    catch(error) {
      console.error("Error fetching current user:", error);
      throw error;
    }
  },

  getCurrentUser: ()=>{
    const user = localStorage.getItem("currentUser");
    return user ? JSON.parse(user) : null;
  },

  isAuthenticated: ()=>{
    const user = localStorage.getItem("currentUser");
    return user ? true : false;
  },

  fetchPrivateMessages: async(user1,user2)=>{
    try {
      const response = await api.get(`/api/messages/private/?user1=${encodeURIComponent(user1)}&user2=${encodeURIComponent(user2)}`);
      return {
        success: true,
        messages: response.data
      };
    } catch (error) {
      console.error("Error fetching private messages:", error);
      throw error;
    }
  }

}


















