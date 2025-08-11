import React from 'react';
import {BrowserRouter as Router , Routes , Route} from "react-router-dom";
import Navbar from "./components/Navbar";
import Mainpage from "./pages/Mainpage";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <Router>
      <div className="App">
        <Navbar/>
        <Routes>
             <Route path="/" element={<Mainpage/>}/>
             <Route path="/login" element={<Login/>}/>
             <Route path="/signup" element={<Signup/>}/>
             <Route path="/chatarea" element={
                  <ProtectedRoute>
                    <Chat/>
                  </ProtectedRoute>
             }/>
             <Route path="*" element={<Navigate to="/" replace/>}/>
        </Routes>

      </div>
    </Router>
  )
}

export default App