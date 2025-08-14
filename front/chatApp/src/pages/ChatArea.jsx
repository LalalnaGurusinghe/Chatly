import { useNavigate } from "react-router-dom";
import { authService } from "../service/authServices";
import React, { useEffect, useState, useRef, useCallback } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import PrivateChat from "./PrivateChat";
import "../styles/Chat.css";

const ChatArea = () => {
  const navigate = useNavigate();
  const currentUser = authService.getCurrentUser();

  // Redirect to login if not authenticated and fetch current user data
  useEffect(() => {
    const authStatus = authService.checkAuthStatus();
    
    if (!authStatus.isAuthenticated) {
      navigate("/login");
      return;
    }
    
    if (!currentUser) {
      navigate("/login");
      return;
    }
    
    // Fetch current user data from backend to ensure we have the latest info
    const fetchUserData = async () => {
      try {
        await authService.refreshUserData();
      } catch (error) {
        console.error("Error fetching current user:", error);
        // If we can't fetch user data, redirect to login
        navigate("/login");
      }
    };
    
    fetchUserData();
  }, [currentUser, navigate]);

  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [isTyping, setIsTyping] = useState("");
  const [privateChat, setPrivateChat] = useState(new Map());
  const [unreadMessages, setUnreadMessages] = useState(new Map());
  const [onlineUsers, setOnlineUsers] = useState(new Set());
  const [isRefreshing, setIsRefreshing] = useState(false);

  const privateMessageHandlers = useRef(new Map());
  const stompClient = useRef(null);
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const typingNotifyTimeoutRef = useRef(null);

  const emojis = [
    "😀",
    "😂",
    "😍",
    "🥰",
    "😎",
    "😭",
    "😡",
    "👍",
    "👎",
    "🙏",
    "🎉",
    "🔥",
    "💯",
    "❤️",
    "✨",
    "🤔",
    "🙌",
    "🎶",
    "📷",
    "🍕",
  ];

  if (!currentUser) return (
    <div className="chat-container">
      <div className="main-chat">
        <div className="chat-header">
          <h4>Authentication Required</h4>
        </div>
        <div className="message-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
          <div className="system-message warning">
            Please log in to access the chat area.
          </div>
        </div>
      </div>
    </div>
  );

  // Check if user is properly authenticated
  const authStatus = authService.checkAuthStatus();
  if (!authStatus.isAuthenticated) {
    navigate("/login");
    return null;
  }

  const { username, color: userColor } = currentUser;

  // Function to refresh current user data
  const refreshCurrentUser = async () => {
    if (isRefreshing) return; // Prevent multiple simultaneous refreshes
    
    setIsRefreshing(true);
    try {
      await authService.refreshUserData();
      // Force re-render by updating state
      window.location.reload();
    } catch (error) {
      console.error("Error refreshing current user:", error);
      navigate("/login");
    } finally {
      setIsRefreshing(false);
    }
  };

  // Scroll to bottom when messages change
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  const formatTime = (ts) => {
    try {
      const d = new Date(ts);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    } catch {
      return "";
    }
  };

  const handleEmojiClick = (emoji) => {
    setMessage((prev) => prev + emoji);
    setShowEmojiPicker(false);
  };

  const handleTyping = (e) => {
    const value = e.target.value;
    setMessage(value);

    // Debounce typing notifications so we don't spam the server
    if (stompClient.current && stompClient.current.active) {
      if (typingNotifyTimeoutRef.current)
        clearTimeout(typingNotifyTimeoutRef.current);
      typingNotifyTimeoutRef.current = setTimeout(() => {
        stompClient.current.publish({
          destination: "/app/chat.typing",
          body: JSON.stringify({
            sender: username,
            type: "TYPING",
            timestamp: new Date().toISOString(),
          }),
        });
      }, 250);
    }
  };

  const sendMessage = (e) => {
    e.preventDefault();
    const trimmed = message.trim();
    if (!trimmed || !stompClient.current || !stompClient.current.active) return;

    const chatMessage = {
      sender: username,
      content: trimmed,
      type: "CHAT",
      color: userColor,
      timestamp: new Date().toISOString(),
    };

    stompClient.current.publish({
      destination: "/app/chat.send",
      body: JSON.stringify(chatMessage),
    });

    setMessage("");
  };

  const openPrivateChat = useCallback(
    (otherUser) => {
      if (!otherUser || otherUser === username) return;
      setPrivateChat((prev) => {
        const next = new Map(prev);
        if (!next.has(otherUser)) next.set(otherUser, []);
        return next;
      });
      // reset unread count for that user
      setUnreadMessages((prev) => {
        const next = new Map(prev);
        next.delete(otherUser);
        return next;
      });
    },
    [username]
  );

  const closePrivateChat = useCallback((otherUser) => {
    setPrivateChat((prev) => {
      const next = new Map(prev);
      next.delete(otherUser);
      return next;
    });
    // also remove any handler
    privateMessageHandlers.current.delete(otherUser);
  }, []);

  const registerPrivateMessageHandler = useCallback((otherUser, handler) => {
    privateMessageHandlers.current.set(otherUser, handler);
  }, []);

  const unregisterPrivateMessageHandler = useCallback((otherUser) => {
    privateMessageHandlers.current.delete(otherUser);
  }, []);

  // Connect STOMP and fetch online users periodically
  useEffect(() => {
    if (!username) return;

    // Function to fetch online users
    const fetchOnlineUsers = async () => {
      try {
        const data = await authService.getOnlineUsers();
        const fetchedUsers = Array.isArray(data)
          ? data
          : data && typeof data === "object"
          ? Object.keys(data)
          : [];
        setOnlineUsers((prev) => {
          const merged = new Set(prev);
          fetchedUsers.forEach((u) => merged.add(u));
          merged.add(username);
          return merged;
        });
      } catch (error) {
        console.error("Error fetching online users", error);
        // If we can't fetch online users, at least show the current user
        setOnlineUsers((prev) => {
          const next = new Set(prev);
          next.add(username);
          return next;
        });
      }
    };

    // Initial fetch
    fetchOnlineUsers();

    // Set up periodic refresh every 30 seconds
    const intervalId = setInterval(fetchOnlineUsers, 30000);

    return () => clearInterval(intervalId);
  }, [username]);

  // Connect STOMP
  useEffect(() => {
    if (!username) return;

    // Ensure current user appears online locally
    setOnlineUsers((prev) => {
      const next = new Set(prev);
      next.add(username);
      return next;
    });

    const client = new Client({
      // reconnect automatically
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      connectHeaders: {
        "client-id": username,
        "session-id": Date.now().toString(),
        username,
      },
      debug: () => {}, // silence logs; set to console.log for debugging
    });

    client.onConnect = async () => {
      console.log("Connected to WebSocket");
      // Subscribe to group channel
      client.subscribe("/topic/group", (messageFrame) => {
        const chatMessage = JSON.parse(messageFrame.body);

        setOnlineUsers((prev) => {
          const next = new Set(prev);
          if (chatMessage.type === "JOIN") next.add(chatMessage.sender);
          else if (chatMessage.type === "LEAVE")
            next.delete(chatMessage.sender);
          return next;
        });

        if (chatMessage.type === "TYPING") {
          setIsTyping(chatMessage.sender);
          clearTimeout(typingTimeoutRef.current);
          typingTimeoutRef.current = setTimeout(() => setIsTyping(""), 3000);
          return;
        }

        if (
          chatMessage.type === "CHAT" ||
          chatMessage.type === "JOIN" ||
          chatMessage.type === "LEAVE"
        ) {
          setMessages((prev) => [
            ...prev,
            {
              ...chatMessage,
              timestamp: chatMessage.timestamp || new Date().toISOString(),
              id: chatMessage.id || `${Date.now()}-${Math.random()}`,
            },
          ]);
        }
      });

      // Subscribe to private queue
      client.subscribe(`/user/${username}/queue/private`, (messageFrame) => {
        const privateMessage = JSON.parse(messageFrame.body);
        const otherUser =
          privateMessage.sender === username
            ? privateMessage.recipient || privateMessage.recepient
            : privateMessage.sender;

        // Invoke active handler if a private window for that user is open
        const handler = privateMessageHandlers.current.get(otherUser);
        if (handler) {
          try {
            handler(privateMessage);
          } catch (err) {
            console.error("Error in private message handler:", err);
          }
        } else if (
          (privateMessage.recipient || privateMessage.recepient) === username
        ) {
          // Increase unread count if the chat window is not open
          setUnreadMessages((prev) => {
            const next = new Map(prev);
            const currentCount = next.get(otherUser) || 0;
            next.set(otherUser, currentCount + 1);
            return next;
          });
        }
      });

      // Announce join
      client.publish({
        destination: "/app/chat.adduser",
        body: JSON.stringify({
          username,
          type: "JOIN",
          color: userColor,
        }),
      });

      // Also add current user to online users list
      setOnlineUsers((prev) => {
        const next = new Set(prev);
        next.add(username);
        return next;
      });

      // Fetch online users from backend
      try {
        const data = await authService.getOnlineUsers();
        const fetchedUsers = Array.isArray(data)
          ? data
          : data && typeof data === "object"
          ? Object.keys(data)
          : [];
        setOnlineUsers((prev) => {
          const merged = new Set(prev);
          fetchedUsers.forEach((u) => merged.add(u));
          merged.add(username);
          return merged;
        });
      } catch (error) {
        console.error("Error fetching online users", error);
        // If we can't fetch online users, at least show the current user
        setOnlineUsers((prev) => {
          const next = new Set(prev);
          next.add(username);
          return next;
        });
      }
    };

    client.onStompError = (frame) => {
      console.error("Broker reported error: " + frame.headers["message"]);
    };

    client.onWebSocketError = (error) => {
      console.error("WebSocket error:", error);
      // Add a fallback message when backend is not available
      setMessages((prev) => [
        ...prev,
        {
          id: `error-${Date.now()}`,
          type: "SYSTEM",
          content: "⚠️ Backend server is not running. Please start the backend server to enable real-time chat.",
          timestamp: new Date().toISOString(),
          sender: "System",
        },
      ]);
    };

    client.activate();
    stompClient.current = client;

    return () => {
      clearTimeout(typingTimeoutRef.current);
      clearTimeout(typingNotifyTimeoutRef.current);
      if (stompClient.current) {
        // Announce leave before disconnecting (best-effort)
        try {
          if (stompClient.current.active) {
            stompClient.current.publish({
              destination: "/app/chat.adduser",
              body: JSON.stringify({
                username,
                type: "LEAVE",
                color: userColor,
              }),
            });
          }
        } catch {}
        stompClient.current.deactivate();
      }
    };
  }, [username, userColor]);

  return (
    <div className="chat-container">
      <div className="sidebar">
        <div className="sidebar-header">
          <h2>Users</h2>
        </div>
        <div className="users-list">
          {Array.from(onlineUsers).map((user) => (
            <div
              key={user}
              className={`user-item ${user === username ? "current-user" : ""}`}
              onClick={() => openPrivateChat(user)}
            >
              <div
                className="user-avatar"
                style={{
                  backgroundColor:
                    user === username ? userColor || "#007bff" : "#007bff",
                }}
              >
                {user?.charAt(0)?.toUpperCase?.() || "?"}
              </div>
              <span>{user}</span>
              {user === username && <span className="you-label">(You)</span>}
              {unreadMessages.has(user) && (
                <span className="unread-count">{unreadMessages.get(user)}</span>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="main-chat">
        <div className="chat-header">
          <h4>Welcome, {username}</h4>
          <button 
            onClick={refreshCurrentUser}
            className="refresh-btn"
            title="Refresh user data"
            disabled={isRefreshing}
          >
            {isRefreshing ? "⏳ Refreshing..." : "🔄 Refresh"}
          </button>
        </div>

        <div className="message-container">
          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`message ${String(msg.type || "").toLowerCase()}`}
            >
              {msg.type === "JOIN" && (
                <div className="system-message">
                  {msg.sender} joined the group
                </div>
              )}

              {msg.type === "LEAVE" && (
                <div className="system-message">
                  {msg.sender} left the group
                </div>
              )}

              {msg.type === "SYSTEM" && (
                <div className={`system-message ${msg.content.includes("⚠️") ? "warning" : ""}`}>
                  {msg.content}
                </div>
              )}

              {msg.type === "CHAT" && (
                <div
                  className={`chat-message ${
                    msg.sender === username ? "own-message" : ""
                  }`}
                >
                  <div className="message-info">
                    <span
                      className="sender"
                      style={{ color: msg.color || "#007bff" }}
                    >
                      {msg.sender}
                    </span>
                    <span className="time">{formatTime(msg.timestamp)}</span>
                  </div>
                  <div className="message-text">{msg.content}</div>
                </div>
              )}
            </div>
          ))}

          {isTyping && isTyping !== username && (
            <div className="typing-indicator">{isTyping} is typing...</div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="input-area">
          {showEmojiPicker && (
            <div className="emoji-picker">
              {emojis.map((emoji) => (
                <button
                  key={emoji}
                  type="button"
                  onClick={() => handleEmojiClick(emoji)}
                >
                  {emoji}
                </button>
              ))}
            </div>
          )}

          <form onSubmit={sendMessage} className="message-form">
            <button
              type="button"
              onClick={() => setShowEmojiPicker((s) => !s)}
              className="emoji-btn"
              aria-label="Toggle emoji picker"
            >
              😀
            </button>

            <input
              type="text"
              placeholder="Type a message..."
              value={message}
              onChange={handleTyping}
              className="message-input"
              maxLength={500}
            />

            <button
              type="submit"
              className="send-btn"
              disabled={!message.trim()}
            >
              Send
            </button>
          </form>
        </div>
      </div>

      {Array.from(privateChat.keys()).map((otherUser) => (
        <PrivateChat
          key={otherUser}
          currentUser={username}
          recepientUser={otherUser}
          userColor={userColor}
          stompClient={stompClient}
          onClose={() => closePrivateChat(otherUser)}
          registerPrivateMessageHandler={registerPrivateMessageHandler}
          unregisterPrivateMessageHandler={unregisterPrivateMessageHandler}
        />
      ))}
    </div>
  );
};

export default ChatArea;
