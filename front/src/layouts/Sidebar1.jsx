import { useEffect, useState } from "react";
import axiosInstance from '@/lib/axiosInstance';
import { toast } from "@/hooks/use-toast";

export default function Sidebar1({ onSelectDM, onSelectServer, onLeaveOrDeleteServer }) {
  const [servers, setServers] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [showJoin, setShowJoin] = useState(false);
  const [serverName, setServerName] = useState("");
  const [joinCode, setJoinCode] = useState("");

  const fetchServers = () =>
    axiosInstance.get("/servers/my").then(res => setServers(res.data));

  useEffect(() => {
    fetchServers();
  }, []);

  // 서버 개설
  const handleCreate = async () => {
    if (!serverName.trim()) return;
    await axiosInstance.post("/servers", { name: serverName });
    setServerName("");
    setShowCreate(false);
    fetchServers();
  };

  // 서버 참여
  const handleJoin = async () => {
    if (!joinCode.trim()) return;
    await axiosInstance.post("/servers/join", { code: joinCode });
    setJoinCode("");
    setShowJoin(false);
    fetchServers();
  };

  // 서버 탈퇴/삭제
  const handleLeaveOrDelete = async (serverId, userRole) => {
    const isAdmin = (userRole?.toUpperCase?.() === "ADMIN"); // 🔑 권한에 따라 분기
    console.log("🧪 서버 ID:", serverId, "역할:", userRole);
    const confirmMsg = isAdmin
      ? "정말 이 서버를 삭제하시겠습니까? 삭제 시 복구할 수 없습니다."
      : "정말 이 서버에서 탈퇴하시겠습니까?";

    if (!window.confirm(confirmMsg)) return;

    try {
      if (isAdmin) {
        await axiosInstance.delete(`/servers/${serverId}`);
      } else {
        await axiosInstance.delete(`/servers/${serverId}/members/leave`);
      }

      
  
      toast({ title: "알림", description: isAdmin ? "서버를 삭제했습니다." : "서버에서 나갔습니다." });
      fetchServers(); // ✅ 성공 시에만
      // 상태 초기화 함수
      onLeaveOrDeleteServer();
    } catch (err) {
      console.error("❌ 서버 탈퇴/삭제 실패", err);
      toast({
        title: "오류 발생",
        description: err.response?.data?.message || err.message || "서버에서 오류가 발생했습니다.",
        variant: "destructive",
      });
    }
  }
  return (
    <div className="w-[72px] bg-[#1e1f22] flex flex-col items-center py-3 gap-2 border-r border-[#232428] h-full">
      {/* DM 버튼 */}
      <div className="relative group">
        <button
          className="w-12 h-12 rounded-[50%] bg-[#5865f2] flex items-center justify-center text-white font-bold text-lg transition-all duration-200 hover:rounded-[16px] hover:bg-[#4752c4] shadow-lg relative overflow-hidden"
          onClick={onSelectDM}
          title="다이렉트 메시지"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
          <div className="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
        </button>
        {/* 호버 툴팁 */}
        <div className="absolute left-16 top-1/2 -translate-y-1/2 bg-black text-white text-sm px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
          다이렉트 메시지
        </div>
      </div>

      {/* 구분선 */}
      <div className="h-[2px] w-8 bg-[#3f4147] rounded-full" />

      {/* 서버 목록 */}
      <div className="flex flex-col gap-2 w-full items-center overflow-y-auto scrollbar-none">
        {servers.map(server => (
          <div key={server.id} className="relative group">
            <button
              className="w-12 h-12 rounded-[50%] bg-[#36393f] text-white font-bold text-lg transition-all duration-200 hover:rounded-[16px] hover:bg-[#5865f2] shadow-md relative overflow-hidden flex items-center justify-center"
              onClick={() => onSelectServer(server.id)}
              title={server.name}
            >
              <span className="relative z-10">
                {server.name.charAt(0).toUpperCase() || "?"}
              </span>
              <div className="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
            </button>
            
            {/* 호버 시 서버 이름 툴팁 */}
            <div className="absolute left-16 top-1/2 -translate-y-1/2 bg-black text-white text-sm px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
              {server.name}
            </div>
            
            {/* 우클릭 메뉴 또는 삭제 버튼 */}
            <button
              onClick={() => handleLeaveOrDelete(server.id, server.role)}
              className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 rounded-full text-white text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-200 hover:bg-red-600 flex items-center justify-center"
              title="서버 탈퇴"
            >
              ×
            </button>
          </div>
        ))}
      </div>

      {/* 서버 추가 버튼들 */}
      <div className="mt-auto flex flex-col gap-2">
        {/* 서버 개설 */}
        <div className="relative group">
          <button
            onClick={() => setShowCreate(true)}
            className="w-12 h-12 rounded-[50%] bg-[#36393f] text-[#3ba55d] font-bold text-2xl hover:rounded-[16px] hover:bg-[#3ba55d] hover:text-white transition-all duration-200 shadow-md relative overflow-hidden flex items-center justify-center"
            title="서버 개설"
          >
            <span className="relative z-10">+</span>
            <div className="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
          </button>
          <div className="absolute left-16 top-1/2 -translate-y-1/2 bg-black text-white text-sm px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
            서버 개설
          </div>
        </div>

        {/* 서버 참여 */}
        <div className="relative group">
          <button
            onClick={() => setShowJoin(true)}
            className="w-12 h-12 rounded-[50%] bg-[#36393f] text-[#3ba55d] font-bold text-lg hover:rounded-[16px] hover:bg-[#3ba55d] hover:text-white transition-all duration-200 shadow-md relative overflow-hidden flex items-center justify-center"
            title="서버 참여"
          >
            <span className="relative z-10">🔍</span>
            <div className="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
          </button>
          <div className="absolute left-16 top-1/2 -translate-y-1/2 bg-black text-white text-sm px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
            서버 참여
          </div>
        </div>
      </div>

      {/* 서버 개설 모달 */}
      {showCreate && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/50 backdrop-blur-sm z-50">
          <div className="bg-[#36393f] p-6 rounded-lg shadow-2xl border border-[#4f545c] w-96">
            <h3 className="text-white text-xl font-bold mb-4">서버 개설하기</h3>
            <p className="text-[#b9bbbe] text-sm mb-4">
              서버는 나와 친구들이 함께 시간을 보낼 수 있는 공간입니다.
            </p>
            <div className="mb-4">
              <label className="block text-[#b9bbbe] text-xs font-bold uppercase mb-2">
                서버 이름
              </label>
              <input
                type="text"
                className="w-full p-3 rounded bg-[#40444b] text-white placeholder-[#72767d] border border-[#4f545c] focus:border-[#5865f2] focus:outline-none transition-colors"
                placeholder="내 멋진 서버"
                value={serverName}
                onChange={e => setServerName(e.target.value)}
                autoFocus
                maxLength={100}
              />
            </div>
            <div className="flex justify-end gap-3">
              <button 
                onClick={() => setShowCreate(false)} 
                className="px-4 py-2 text-white hover:underline transition-all"
              >
                취소
              </button>
              <button 
                onClick={handleCreate} 
                className="px-6 py-2 bg-[#5865f2] text-white rounded hover:bg-[#4752c4] transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={!serverName.trim()}
              >
                개설하기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 서버 참여 모달 */}
      {showJoin && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/50 backdrop-blur-sm z-50">
          <div className="bg-[#36393f] p-6 rounded-lg shadow-2xl border border-[#4f545c] w-96">
            <h3 className="text-white text-xl font-bold mb-4">서버에 참여하기</h3>
            <p className="text-[#b9bbbe] text-sm mb-4">
              초대 링크나 초대 코드를 입력하여 기존 서버에 참여하세요.
            </p>
            <div className="mb-4">
              <label className="block text-[#b9bbbe] text-xs font-bold uppercase mb-2">
                초대 링크 또는 코드
              </label>
              <input
                type="text"
                className="w-full p-3 rounded bg-[#40444b] text-white placeholder-[#72767d] border border-[#4f545c] focus:border-[#5865f2] focus:outline-none transition-colors"
                placeholder="https://discord.gg/example 또는 hTKzmak"
                value={joinCode}
                onChange={e => setJoinCode(e.target.value)}
                autoFocus
              />
            </div>
            <div className="flex justify-end gap-3">
              <button 
                onClick={() => setShowJoin(false)} 
                className="px-4 py-2 text-white hover:underline transition-all"
              >
                취소
              </button>
              <button 
                onClick={handleJoin} 
                className="px-6 py-2 bg-[#5865f2] text-white rounded hover:bg-[#4752c4] transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={!joinCode.trim()}
              >
                서버 참여하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}