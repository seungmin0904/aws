import { useEffect, useRef, useState } from "react";
import axiosInstance from '@/lib/axiosInstance';

export default function ChatRoom({ roomId, currentUser, subscribe, send , connected }) {
  const [messageMap, setMessageMap] = useState({});
  const [input, setInput] = useState("");
  const messages = messageMap[roomId] || [];

  const scrollRef = useRef(null);

  // 새 메시지 올 때마다 자동 스크롤 아래로
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  // 메시지 로딩
  useEffect(() => {
    if (!roomId) return;
    if (messageMap[roomId]) return;

    axiosInstance.get(`/chat/${roomId}`)
      .then(res => {
        setMessageMap(prev => ({
          ...prev,
          [roomId]: res.data || []
        }));
      })
      .catch(() => {
        setMessageMap(prev => ({
          ...prev,
          [roomId]: []
        }));
      });
  }, [roomId]);

  // 구독
  useEffect(() => {
    if (!roomId || !connected) return;
    const sub = subscribe(`/topic/chatroom.${roomId}`, payload => {
      setMessageMap(prev => ({
        ...prev,
        [roomId]: [...(prev[roomId] || []), payload]
      }));
    });
    return () => sub?.unsubscribe?.();
  }, [roomId, subscribe , connected]);

  function sendMessage() {
    if (!input.trim()) return;
    send(`/app/chat.send/${roomId}`, {
      message: input,
    });
    setInput("");
  }

  return (
    <div className="w-full h-full flex flex-col min-h-0">
      {/* 채팅 메시지 목록 (스크롤 가능 영역) */}
      <div
        ref={scrollRef}
        className="flex-1 overflow-y-auto p-4 bg-[#313338] text-white min-h-0"
        style={{ height: 0 }}
      >
        {messages.map((msg, i) => (
          <div key={i} className="mb-2">
            {msg.sender && <span className="font-bold text-blue-400">{msg.sender}:</span>}{" "}
            <span className="text-gray-200">{msg.message}</span>
          </div>
        ))}
      </div>

      {/* 입력창 (하단 고정) */}
      <div className="flex-shrink-0 flex gap-2 p-4 bg-[#2b2d31] border-t border-[#1e1f22]">
        <input
          className="flex-1 bg-[#383a40] rounded-lg px-4 py-2 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && sendMessage()}
          placeholder="메시지 입력..."
        />
        <button
          className="bg-blue-600 hover:bg-blue-700 text-white rounded-lg px-6 py-2 font-medium transition-colors"
          onClick={sendMessage}
        >
          전송
        </button>
      </div>
    </div>
  );
}