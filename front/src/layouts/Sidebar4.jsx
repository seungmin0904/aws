import { useEffect } from "react";
import { useRealtime } from "@/context/RealtimeContext"; // RealtimeContext에서 상태 사용
import clsx from "clsx";

export default function Sidebar4({ serverId, selectedMemberId, onSelectMember }) {
  const { state, fetchAndSetServerMembers } = useRealtime();
  const members = state.serverMembers[serverId] || [];
  const loading = state.loadingServerMembers.has(serverId);

  useEffect(() => {
    if (!serverId) return;

    // ✅ 서버 멤버 초기 로딩 요청 (처음 mount 시 or 서버 변경 시)
    fetchAndSetServerMembers(serverId);
  }, [serverId]);

  if (!serverId) return null;

  return (
    <div className="w-[300px] min-w-[260px] max-w-[260px] bg-[#232428] border-l border-[#232428] flex flex-col h-full">
      <div className="font-bold text-base px-5 py-4 border-b border-[#232428]">참여자</div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center text-zinc-400">
          로딩중...
        </div>
      ) : members.length === 0 ? (
        <div className="flex-1 flex items-center justify-center text-zinc-500">
          참여자가 없습니다
        </div>
      ) : (
        <ul className="flex-1 overflow-y-auto px-3 py-2">
          {members.map(m => (
            <li
              key={m.memberId || m.id}
              className={clsx(
                "flex items-center gap-2 px-2 py-2 rounded hover:bg-zinc-800 cursor-pointer",
                selectedMemberId === (m.memberId || m.id) && "bg-zinc-800 font-semibold"
              )}
              onClick={() => onSelectMember && onSelectMember(m)}
            >
              <img
                src={m.profile || "/default-profile.png"}
                alt="profile"
                className="w-9 h-9 rounded-full object-cover border border-zinc-700"
              />
              <div>
                <div className="text-sm">{m.name}</div>
                <div className="text-xs text-zinc-400">{m.role}</div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
