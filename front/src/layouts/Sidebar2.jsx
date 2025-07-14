import { useEffect, useState, useRef } from "react";
import axiosInstance from '@/lib/axiosInstance';
import { useUser } from "@/context/UserContext";
import useMediasoupClient from "@/hooks/useMediaSoupClient";
import { useRealtime } from "@/context/RealtimeContext";

export default function Sidebar2({
  dmMode,
  serverId,
  onSelectFriendPanel,
  onSelectDMRoom,
  onSelectChannel
}) {
  const { user } = useUser();
  const currentUserId = user?.id;
  const {
    createSendTransport,
    sendAudio,
    createRecvTransport,
    consumeSpecificAudio,
    joinVoiceChannel,
    leaveVoiceChannel,
    voiceParticipantsMap,
    speakingUserIds,
    micVolume,
  } = useMediasoupClient(user?.id, user?.nickname);
  
 useEffect(() => {
  console.log("🎯 userContext 값:", user);
}, []);
  

  const [friends, setFriends] = useState([]);
  const [channels, setChannels] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState("");
  const [newType, setNewType] = useState("TEXT");
  const [inviteCode, setInviteCode] = useState("");
  const [inviteChannelId, setInviteChannelId] = useState(null);
  const initialLoadRef = useRef(false);
  
  // ✅ RealtimeContext에서 dmRooms와 refreshDmRooms 가져오기
  const { state, dispatch, ready, refreshDmRooms } = useRealtime();
  const dmRooms = state.dmRooms;

  useEffect(() => {
    if (dmMode) {
      axiosInstance.get("/friends")
        .then(res => setFriends(Array.isArray(res.data) ? res.data : []))
        .catch(() => setFriends([]));
    }
  }, [dmMode]);

  useEffect(() => {
    if (!dmMode && serverId) fetchChannels();
    else setChannels([]);
  }, [dmMode, serverId]);

  function fetchChannels() {
    axiosInstance.get(`/servers/${serverId}/channels`)
      .then(res => setChannels(Array.isArray(res.data) ? res.data : []))
      .catch(() => setChannels([]));
  }

  // ✅ DM 모드일 때만 DM 목록 초기 로딩 (RealtimeContext에서 관리)
  useEffect(() => {
    if (dmMode && user?.id && ready) {
        console.log("🟢 Sidebar2: 최초 DM 목록 로드 수행");
        refreshDmRooms?.();
        initialLoadRef.current = true;
    }
  }, [dmMode, user?.id, ready]);

  // ✅ 디버깅용 로그 추가
  useEffect(() => {
    console.log("📋 Sidebar2 DM 목록 상태:", {
      dmRoomsCount: dmRooms.length,
      dmRooms: dmRooms,
      ready: ready,
      dmMode: dmMode
    });
  }, [dmRooms, ready, dmMode]);

  function handleCreateChannel() {
    if (!newName.trim()) return;
    if (!serverId) {
      alert("serverId가 비어있음. 서버를 선택해주세요.");
      return;
    }
    if (!dmMode) {
      axiosInstance.post(`/chatrooms`, {
        name: newName,
        type: newType,
        description: "",
        serverId,
        roomType: "SERVER",
      }).then(() => {
        setShowCreate(false);
        setNewName("");
        setNewType("TEXT");
        fetchChannels();
      });
    }
  }

  function handleDeleteChannel(channelId) {
    if (!window.confirm("정말 삭제할까요?")) return;
    axiosInstance.delete(`/chatrooms/${channelId}`)
    .then(() => {
      setChannels(prev => prev.filter(ch => ch.id !== channelId));
      fetchChannels(); // 💡 여기에서 강제 리렌더
    })
    .catch((err) => {
      console.error("❌ 삭제 실패", err);
    });
}
  function handleInviteCode(serverId) {
    console.log("📨 invite 요청 serverId:", serverId);
    axiosInstance.post(`/invites`, {
    serverId: serverId,   // ✅ 필수 값
    expireAt: null,      // ✅ 선택 값 (무제한일 경우 null)
    maxUses: null,       // ✅ 선택 값 (무제한일 경우 null)
    memo: ""             // ✅ 선택 값 (없으면 빈 문자열)
  })
  .then(res => {
    setInviteCode(res.data.code || res.data.inviteCode || "");
  })
  .catch(err => {
    console.error("❌ 초대코드 생성 실패", err?.response?.data || err.message);
    alert("초대코드 생성에 실패했습니다.");
  });
}

  function closeInviteModal() {
    setInviteCode("");
    setInviteChannelId(null);
  }

  function handleDeleteDmRoom(roomId) {
    if (!window.confirm("이 DM방을 목록에서 삭제하시겠습니까?")) return;

    axiosInstance.delete(`/dm/room/${roomId}/hide/${currentUserId}`)
      .then(() => {
        // ✅ 전역 상태에서도 즉시 제거
        dispatch({
          type: "SET_DM_ROOMS",
          payload: state.dmRooms.filter((room) => room.id !== roomId)
        });
        console.log("✅ DM방 삭제 완료 - UI에서 즉시 제거됨");
      })
      .catch(err => {
        console.error("❌ DM 삭제 실패:", err);
        alert("DM 삭제에 실패했습니다.");
      });
  }

  const textChannels = channels.filter(ch => (ch?.type || '').toUpperCase().trim() === "TEXT");
  const voiceChannels = channels.filter(ch => (ch?.type || '').toUpperCase().trim() === "VOICE");

  if (dmMode) {
    return (
      <div className="w-[260px] min-w-[200px] max-w-[320px] flex flex-col h-full bg-[#2b2d31] border-r border-[#232428]">
        <div className="border-b border-[#232428] p-2">
          <button
            className="w-full text-left px-3 py-2 rounded text-white bg-[#2b2d31] hover:bg-[#36393f] transition font-bold"
            onClick={() => onSelectFriendPanel(null)}
          >
            친구
          </button>
        </div>
        <div className="flex items-center justify-between px-4 py-3">
          <div className="text-xs text-zinc-400 font-bold">다이렉트 메시지</div>
          <button
            onClick={refreshDmRooms}
            className="text-xs text-zinc-500 hover:text-white transition"
            title="DM 목록 새로고침"
          >🔄</button>
        </div>
        <ul className="px-2 flex-1 overflow-y-auto">
          {!ready && <li className="px-3 py-2 text-zinc-500 text-sm">연결 중...</li>}
          {ready && dmRooms.length === 0 && <li className="px-3 py-2 text-zinc-500 text-sm">DM 목록이 없습니다</li>}
          {dmRooms?.filter(room => room.visible).map((room) => (
            <li
              key={room.id}
              className="px-3 py-2 rounded group flex text-white items-center justify-between hover:bg-zinc-800 cursor-pointer transition"
              onClick={() => onSelectDMRoom(room.id)}
            >
              <span className="text-base truncate flex-1">{room?.name || "이름없음"}</span>
              <button
                className="dm-delete-btn text-zinc-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition ml-2 flex-shrink-0"
                onClick={(e) => { e.stopPropagation(); handleDeleteDmRoom(room.id); }}
                title="DM 삭제"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div className="w-[280px] min-w-[280px] max-w-[280px] h-full bg-[#2b2d31] flex flex-col border-r border-[#232428]">
      <div className="flex-1 flex flex-col">
      <button className="text-xs bg-zinc-700 text-white rounded px-2 py-2 ml-1" onClick={e => { e.stopPropagation(); handleInviteCode(serverId); }}>서버 초대</button>
        <div className="flex items-center justify-between px-4 mt-4 mb-1">
          <span className="text-xs text-zinc-400 font-bold">텍스트 채널</span>
          <button className="text-xs text-[#3ba55d] bg-[#232428] rounded px-2 py-1 ml-2" onClick={() => { setNewType("TEXT"); setShowCreate(true); }} title="채널 생성">＋</button>
        </div>
        <ul className="mb-3 px-2">
          {textChannels.length === 0 && <div className="text-zinc-500 px-2 py-2">없음</div>}
          {textChannels.map((ch, i) => (
            <li key={ch.id ?? `textch-${i}`} className="flex items-center gap-2 px-2 py-2 rounded text-white hover:bg-zinc-800 group cursor-pointer transition" onClick={() => onSelectChannel && onSelectChannel(ch.id)}>
              <span className="text-[#8e9297] font-bold">#</span>
              <span className="flex-1">{ch?.name || "이름없음"}</span>
              <button className="text-[11px] text-red-400 border border-red-400 rounded-sm px-[4px] py-[1px] leading-tight opacity-0 group-hover:opacity-100 transition bg-[#2b2d31]" onClick={e => { e.stopPropagation(); handleDeleteChannel(ch.id); }} title="채널 삭제"></button>
            </li>
          ))}
        </ul>

        <div className="flex items-center justify-between px-4 mt-2 mb-1">
          <span className="text-xs text-zinc-400 font-bold">음성 채널</span>
          <button className="text-xs text-[#3ba55d] bg-[#232428] rounded px-2 py-1 ml-2" onClick={() => { setNewType("VOICE"); setShowCreate(true); }} title="음성 채널 생성">＋</button>
        </div>
        <ul className="px-2">
          {voiceChannels.length === 0 && <div className="text-zinc-500 px-2 py-2">없음</div>}
          {voiceChannels.map((ch, i) => (
            <li key={ch.id ?? `voicech-${i}`} className="flex flex-col px-2 py-2 rounded text-white hover:bg-zinc-800 group cursor-pointer transition">
              <div className="flex items-center gap-2" onClick={async () => {
                if (onSelectChannel) onSelectChannel(ch.id);
                try {
                  await joinVoiceChannel(ch.id);
                  await createSendTransport();
                  await sendAudio();
                  await createRecvTransport();
                } catch (err) {
                  console.error("❌ 음성 송수신 실패:", err);
                }
              }}>
                <span>🔊</span>
                <span className="flex-1">{ch?.name || "이름없음"}</span>
                <button className="text-[11px] text-red-400 border border-red-400 rounded-sm px-[4px] py-[1px] leading-tight opacity-0 group-hover:opacity-100 transition bg-[#2b2d31]" onClick={e => { e.stopPropagation(); handleDeleteChannel(ch.id); }} title="채널 삭제"></button>
              </div>
              {voiceParticipantsMap.get(ch.id)?.map(({ userId, nickname }) => (
  <div
    key={userId}
    className={`group flex items-center justify-between ml-3 mr-1 mt-0.5 px-2 py-[2px] rounded hover:bg-[#35373c]
      ${userId === currentUserId ? 'bg-[#40444b]' : ''}
      ${speakingUserIds.has(userId) ? 'border border-green-400' : 'border border-transparent'}`}
  >
    <div className="flex items-center gap-2 min-w-0 flex-1">
      <div className="w-[18px] h-[18px] rounded-full bg-[#4f545c] text-white text-[10px] flex items-center justify-center font-bold">
        {nickname?.[0]?.toUpperCase() || "?"}
      </div>
      <span className="text-[13px] text-white leading-tight">
        {nickname}
        {userId === currentUserId && (
          <span className="text-[13px] text-zinc-400 ml-1">(나)</span>
        )}
      </span>
      <svg className="w-5 h-5 text-green-400 ml-1" fill="currentColor" viewBox="0 0 20 20">
        <path d="M10 2a2 2 0 00-2 2v6a2 2 0 104 0V4a2 2 0 00-2-2z" />
        <path d="M4 10a6 6 0 0012 0h-1.5a4.5 4.5 0 01-9 0H4z" />
      </svg>
    </div>
    {userId === currentUserId && (
      <button
        onClick={(e) => {
          e.stopPropagation();
          leaveVoiceChannel();
        }}
        className="text-[13px] bg-transparent text-red-400 hover:text-red-300 opacity-0 group-hover:opacity-100 transition"
        title="나가기"
      >
        −
      </button>
    )}
  </div>
))}
            </li>
          ))}
        </ul>
        {micVolume > 0 && (
  <div className="px-4 pb-2 mt-2">
    <div className="text-xs text-zinc-400">내 마이크</div>
    <div className="h-2 bg-zinc-700 rounded overflow-hidden mt-1">
      <div
        className="h-full bg-green-400 transition-all duration-100"
        style={{ width: `${Math.min(micVolume, 100)}%` }}
      />
    </div>
  </div>
)}
      </div>

      {showCreate && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-40">
          <div className="bg-zinc-900 p-4 rounded w-80 flex flex-col gap-2">
            <div className="text-white font-bold mb-2">채널 개설</div>
            <input className="p-2 rounded" value={newName} onChange={e => setNewName(e.target.value)} placeholder="채널명" />
            <div className="flex gap-2 mt-2">
              <button onClick={handleCreateChannel} className="flex-1 bg-blue-600 text-white rounded py-1">생성</button>
              <button onClick={() => setShowCreate(false)} className="flex-1 bg-zinc-700 text-white rounded py-1">취소</button>
            </div>
          </div>
        </div>
      )}

      {inviteCode && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-zinc-900 p-5 rounded-xl w-[360px] flex flex-col gap-4 shadow-lg">
            <div className="text-white font-bold text-lg">📨 초대 코드</div>
            <div className="flex items-center justify-between bg-zinc-800 px-4 py-2 rounded">
              <span className="font-mono text-white text-base">{inviteCode}</span>
              <button onClick={() => navigator.clipboard.writeText(inviteCode)} className="text-sm bg-blue-600 hover:bg-blue-700 text-white px-2 py-1 rounded">코드 복사</button>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-zinc-300">초대 링크</label>
              <input className="w-full bg-zinc-800 text-white text-sm px-3 py-2 rounded" readOnly value={`${import.meta.env.VITE_BASE_URL || window.location.origin}/invite/${inviteCode}`} />
              <div className="flex justify-end">
                <button onClick={() => navigator.clipboard.writeText(`${import.meta.env.VITE_BASE_URL || window.location.origin}/invite/${inviteCode}`)} className="text-sm bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded">링크 복사</button>
              </div>
            </div>
            <button onClick={closeInviteModal} className="mt-2 bg-zinc-700 text-white px-3 py-1 rounded hover:bg-zinc-600">닫기</button>
          </div>
        </div>
      )}
    </div>
  );
}
