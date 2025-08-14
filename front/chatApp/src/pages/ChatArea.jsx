import {useNavigate} from 'react-router-dom';
import authService from '../service/authServices';
import { useEffect, useState , useRef, use } from 'react';  



const ChatArea = ()=>{

  const navigate = useNavigate();
  const currentUser = authService.getcurrentUser();

  useEffect(()=>{
    if(!currentUser){
      navigate('/login');
      return;
    }
  }, [currentUser, navigate]);

  const [message,setMessage] = useState('');
  const [messages, setMessages] = useState([]);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [isTyping, setIsTyping] = useState('');
  const [privateChat, setPrivateChat] = useState(new Map());
  const [unreadMessages, setUnreadMessages] = useState(new Map());

  const[onlineUsers, setOnlineUsers] = useState(new Set());

  const privateMessageHandlers = useRef(new Map());
  const stompClient = useRef(null);
  const messageEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);


}
