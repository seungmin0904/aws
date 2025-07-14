import { useState, useEffect } from "react";
import axiosInstance from '@/lib/axiosInstance';
import { useNavigate } from "react-router-dom";
import { useUser } from "@/context/UserContext";

export default function FriendDropdown({ userId, userName, x, y, onClose, onSelectDMRoom }) {
  const [status, setStatus] = useState("LOADING");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState(null);
  const { user } = useUser();
  const myId = user?.id;
  const navigate = useNavigate();
  const { refreshDmRooms } = useRealtime();

  // 친구 상태 조회
  useEffect(() => {
    let ignore = false;
    setStatus("LOADING");
    axiosInstance.get(`/friends/status/${userId}`)
      .then(res => {
        if (!ignore) setStatus(res.data.status);
      })
      .catch(() => {
        if (!ignore) setStatus("ERROR");
      });
    return () => { ignore = true; };
  }, [userId]);

  // 친구 추가 핸들러
  const handleAddFriend = async () => {
    setLoading(true);
    setErr(null);
    try {
      await axiosInstance.post("/friends", { targetMemberId: userId });
      setStatus("REQUESTED");
    } catch (e) {
      setErr("신청 실패");
    }
    setLoading(false);
  };

  // 1:1 DM 생성 핸들러
  const handleStartDM = async () => {
    if (!myId) {
      console.error("❌ 내 userId (myId)가 null입니다. UserContext 확인 필요");
      return;
    }

    try {
      const res = await axiosInstance.post("/dm/room", {
        myId: myId,
        friendId: userId,
      });
      console.log("DM 요청 → 내 ID:", myId, "상대 ID:", userId);
      const roomId = res.data.id;
      refreshDmRooms?.();

      if (onSelectDMRoom) onSelectDMRoom(roomId);
      onClose();
    } catch (err) {
      console.error("❌ DM 생성 실패", err);
    }
  };

  // 상태별 아이콘과 색상
  const getStatusInfo = () => {
    switch (status) {
      case "LOADING":
        return { icon: "⏳", text: "확인중...", color: "text-[#b9bbbe]" };
      case "ERROR":
        return { icon: "❌", text: "상태 확인 오류", color: "text-[#f23f43]" };
      case "NONE":
        return { icon: "👤", text: "친구 추가", color: "text-[#b9bbbe]" };
      case "REQUESTED":
        return { icon: "📤", text: "신청 보냄", color: "text-[#faa61a]" };
      case "ACCEPTED":
        return { icon: "✅", text: "이미 친구임", color: "text-[#3ba55d]" };
      case "REJECTED":
        return { icon: "🚫", text: "차단/거절됨", color: "text-[#72767d]" };
      default:
        return { icon: "❓", text: "알 수 없음", color: "text-[#72767d]" };
    }
  };

  const statusInfo = getStatusInfo();

  return (
    <>
      {/* 배경 오버레이 */}
      <div 
        className="fixed inset-0 z-40"
        onClick={onClose}
      />
      
      {/* 드롭다운 메뉴 */}
      <div
        className="fixed z-50 bg-[#18191c] text-white rounded-lg shadow-2xl border border-[#2f3136] min-w-[200px] overflow-hidden"
        style={{ left: x, top: y }}
      >
        {/* 헤더 */}
        <div className="px-3 py-3 bg-[#2f3136] border-b border-[#3f4147]">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-[#5865f2] rounded-full flex items-center justify-center text-white font-bold text-sm">
              {userName.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1">
              <div className="font-semibold text-[#f2f3f5] text-sm">{userName}</div>
              <div className="text-xs text-[#b9bbbe]">사용자</div>
            </div>
          </div>
        </div>

        {/* 메뉴 아이템들 */}
        <div className="py-2">
          {/* 1:1 대화 시작 버튼 */}
          <button
            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-[#b9bbbe] hover:bg-[#4752c4] hover:text-white transition-colors duration-150 group"
            onClick={handleStartDM}
          >
            <span className="text-base">💬</span>
            <span className="font-medium">메시지 보내기</span>
          </button>

          {/* 친구 상태에 따른 버튼 */}
          {status === "NONE" && (
            <button
              className="w-full flex items-center gap-3 px-3 py-2 text-sm text-[#b9bbbe] hover:bg-[#3ba55d] hover:text-white transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={handleAddFriend}
              disabled={loading}
            >
              <span className="text-base">👤</span>
              <span className="font-medium">
                {loading ? "신청 중..." : "친구 추가"}
              </span>
              {loading && (
                <div className="ml-auto">
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                </div>
              )}
            </button>
          )}

          {/* 친구 상태 표시 */}
          {status !== "NONE" && status !== "LOADING" && (
            <div className={`flex items-center gap-3 px-3 py-2 text-sm ${statusInfo.color} cursor-default`}>
              <span className="text-base">{statusInfo.icon}</span>
              <span className="font-medium">{statusInfo.text}</span>
            </div>
          )}

          {/* 로딩 상태 */}
          {status === "LOADING" && (
            <div className="flex items-center gap-3 px-3 py-2 text-sm text-[#b9bbbe]">
              <div className="w-4 h-4 border-2 border-[#b9bbbe]/30 border-t-[#b9bbbe] rounded-full animate-spin"></div>
              <span className="font-medium">상태 확인 중...</span>
            </div>
          )}

          {/* 구분선 */}
          <div className="my-1 mx-2 h-px bg-[#3f4147]"></div>

          {/* 추가 옵션들 */}
          <button
            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-[#b9bbbe] hover:bg-[#36393f] hover:text-white transition-colors duration-150"
            onClick={() => {
              navigator.clipboard.writeText(`사용자 ID: ${userId}`);
              onClose();
            }}
          >
            <span className="text-base">📋</span>
            <span className="font-medium">ID 복사</span>
          </button>

          <button
            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-[#f23f43] hover:bg-[#f23f43] hover:text-white transition-colors duration-150"
            onClick={() => {
              // 신고 기능은 추후 구현
              console.log("신고 기능");
              onClose();
            }}
          >
            <span className="text-base">⚠️</span>
            <span className="font-medium">신고</span>
          </button>
        </div>

        {/* 에러 메시지 */}
        {err && (
          <div className="px-3 py-2 bg-[#f23f43]/10 border-t border-[#f23f43]/20">
            <div className="flex items-center gap-2 text-[#f23f43] text-sm">
              <span>❌</span>
              <span>{err}</span>
            </div>
          </div>
        )}
      </div>
    </>
  );
}