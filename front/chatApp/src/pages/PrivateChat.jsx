import { useEffect, useRef, useState, useCallback } from "react";
import "../styles/PrivateChat.css";

const PrivateChat = ({
  currentUser,
  recepientUser,
  userColor,
  stompClient,
  onClose,
  registerPrivateMessageHandler,
  unregisterPrivateMessageHandler,
}) => {
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const messagesEndRef = useRef();
  const messageIdRef = useRef(new Set());

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  const createMessageId = (msg) => {
    return `${msg.sender}-${msg.recipient}-${msg.content}-${msg.timestamp}`;
  };

  const handleIncomingPrivateMessage = useCallback((msg) => {
    const messageId = msg.id || createMessageId(msg);
    if (!messageIdRef.current.has(messageId)) {
      messageIdRef.current.add(messageId);
      setMessages((prev) => [...prev, { ...msg, id: messageId }]);
    }
  }, []);

  const sendPrivateMessage = (e) => {
    e.preventDefault();
    if (!message.trim()) return;

    const msgObj = {
      sender: currentUser,
      recipient: recepientUser,
      content: message.trim(),
      timestamp: new Date().toISOString(),
      type: "PRIVATE",
      color: userColor,
    };

    stompClient.current.send("/app/chat.private", {}, JSON.stringify(msgObj));

    handleIncomingPrivateMessage(msgObj); // Add message to UI immediately
    setMessage("");
  };

  useEffect(() => {
    let isMounted = true;

    const loadMessageHistory = async () => {
      try {
        const response = await fetch(
          `http://localhost:8080/api/messages/private?user1=${currentUser}&user2=${recepientUser}`
        );

        if (response.ok && isMounted) {
          const history = await response.json();
          const processedHistory = history.map((msg) => {
            const messageId = msg.id || createMessageId(msg);
            return { ...msg, id: messageId };
          });

          messageIdRef.current.clear();
          processedHistory.forEach((msg) => messageIdRef.current.add(msg.id));
          setMessages(processedHistory);
        }
      } catch (error) {
        console.error("Error loading message history:", error);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadMessageHistory();
    registerPrivateMessageHandler(recepientUser, handleIncomingPrivateMessage);

    return () => {
      isMounted = false;
      unregisterPrivateMessageHandler(recepientUser);
    };
  }, [
    currentUser,
    recepientUser,
    registerPrivateMessageHandler,
    unregisterPrivateMessageHandler,
    handleIncomingPrivateMessage,
  ]);

  return (
    <div className="private-chat-window">
      <div className="private-chat-header">
        <div className="recipient-info">
          <div className="recipient-avatar">
            {recepientUser.charAt(0).toUpperCase()}
          </div>
          <h3>{recepientUser}</h3>
        </div>
        <button onClick={onClose} className="close-btn">
          Close
        </button>
      </div>

      <div className="private-message-container">
        {loading ? (
          <p>Loading messages...</p>
        ) : messages.length === 0 ? (
          <p>No messages yet. Start the conversation!</p>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`message ${
                msg.sender === currentUser ? "own-message" : "received-message"
              }`}
            >
              <div className="message-header">
                <span
                  className="sender-name"
                  style={{ color: msg.color || "#6b73FF" }}
                >
                  {msg.sender === currentUser ? "You" : msg.sender}
                </span>
                <span className="timestamp">{formatTime(msg.timestamp)}</span>
              </div>
              <div className="message-content">{msg.content}</div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="private-message-input-container">
        <form onSubmit={sendPrivateMessage} className="private-message-form">
          <input
            type="text"
            placeholder={`Message ${recepientUser}...`}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            className="private-message-input"
            maxLength={500}
          />
          <button
            type="submit"
            disabled={!message.trim()}
            className="private-send-button"
          >
            Send
          </button>
        </form>
      </div>
    </div>
  );
};

export default PrivateChat;
