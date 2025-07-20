import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar1 from './Sidebar1';
import Sidebar2 from './Sidebar2';
import Sidebar3 from './Sidebar3';
import Sidebar4 from './Sidebar4';
import NotificationCenter from '@/components/notification/NotificationCenter';
import { useUser } from '@/context/UserContext';
import { useSocket } from "@/context/WebSocketContext";

export default function MainLayout() {
  const [selectedDM, setSelectedDM] = useState(false);
  const [selectedServerId, setSelectedServerId] = useState(null);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [friendMode, setFriendMode] = useState(false);
  const { user } = useUser();
  const { subscribe, send, connected } = useSocket(user?.token);

  console.log("▶️ MainLayout user:", user);
  console.log("▶️ MainLayout token:", user?.token);
  
  // 로컬스토리지에서 이전 선택 복원
  useEffect(() => {
    const sd = localStorage.getItem('selectedDM') === 'true';
    setSelectedDM(sd);
    setSelectedServerId(sd ? null : Number(localStorage.getItem('selectedServerId')));
    setSelectedRoomId(Number(localStorage.getItem('selectedRoomId')));
    setFriendMode(localStorage.getItem('friendMode') === 'true');
  }, []);

  // DM/서버/채널/친구 패널 선택 핸들러들
  function handleSelectDM() {
    setSelectedDM(true);
    setSelectedServerId(null);
    setSelectedRoomId(null);
    setFriendMode(false);
    localStorage.setItem('selectedDM', 'true');
    localStorage.removeItem('selectedServerId');
    localStorage.removeItem('selectedRoomId');
    localStorage.setItem('friendMode', 'false');
  }
  function handleSelectServer(id) {
    setSelectedDM(false);
    setSelectedServerId(id);
    setSelectedRoomId(null);
    setFriendMode(false);
    localStorage.setItem('selectedDM', 'false');
    localStorage.setItem('selectedServerId', String(id));
    localStorage.removeItem('selectedRoomId');
    localStorage.setItem('friendMode', 'false');
  }
  function handleSelectChannel(id) {
    setSelectedRoomId(id);
    setFriendMode(false);
    localStorage.setItem('selectedRoomId', String(id));
    localStorage.setItem('friendMode', 'false');
  }
  function handleSelectFriendPanel() {
    setSelectedRoomId(null);
    setFriendMode(true);
    localStorage.removeItem('selectedRoomId');
    localStorage.setItem('friendMode', 'true');
  }
  function handleSelectDMRoom(id) {
    setSelectedRoomId(id);
    setFriendMode(false);
    localStorage.setItem('selectedRoomId', String(id));
    localStorage.setItem('friendMode', 'false');
  }

  // 서버 탈퇴/삭제 시 상태 초기화 함수
  function resetServerSelection() {
  setSelectedDM(false);
  setSelectedServerId(null);
  setSelectedRoomId(null);
  setFriendMode(false);

  localStorage.removeItem("selectedDM");
  localStorage.removeItem("selectedServerId");
  localStorage.removeItem("selectedRoomId");
  localStorage.removeItem("friendMode");
  }

  return (
    <div className="fixed inset-0 flex flex-col pt-16 ">
      <div className="flex flex-1 min-h-0">
        <Sidebar1
          onSelectDM={handleSelectDM}
          onSelectServer={handleSelectServer}
          onLeaveOrDeleteServer={resetServerSelection}
        />
        <Sidebar2
          dmMode={selectedDM}
          serverId={selectedServerId}
          onSelectFriendPanel={handleSelectFriendPanel}
          onSelectDMRoom={handleSelectDMRoom}
          onSelectChannel={handleSelectChannel}
        />
        <div className="flex flex-1 min-h-0">
          <Sidebar3
            dmMode={selectedDM}
            serverId={selectedServerId}
            roomId={selectedRoomId}
            friendMode={friendMode}
            subscribe={subscribe}
            send={send}
            currentUser={user}
            connected={connected}
          />
        </div>
        <div className="text-white">
        <Sidebar4 serverId={selectedServerId} roomId={selectedRoomId} />
        </div>
      </div>
    </div>
  );
}