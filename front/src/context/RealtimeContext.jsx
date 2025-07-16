import { useState, createContext, useContext, useReducer, useEffect, useRef } from 'react';
import axiosInstance from '@/lib/axiosInstance';
import { useUser } from './UserContext';
import { usePing } from '@/hooks/usePing';
import { useToast } from "@/hooks/use-toast";

const RealtimeContext = createContext();

const initialState = {
  onlineUsers: new Set(),
  notifications: [],
  typingUsers: new Map(),
  readStatus: new Map(),
  receivedRequests: [],
  sentRequests: [],
  friends: [],
  dmRooms: [],
  serverMembers: {},
  loadingServerMembers: new Set(),
};

function realtimeReducer(state, action) {
  switch (action.type) {
    case 'SET_ONLINE_USERS':
      return { ...state, onlineUsers: new Set(action.payload) };
    case 'USER_STATUS_CHANGE': {
      const newSet = new Set(state.onlineUsers);
      if (action.payload.status === 'ONLINE') newSet.add(action.payload.username);
      else if (action.payload.status === 'OFFLINE') newSet.delete(action.payload.username);
      return { ...state, onlineUsers: newSet };
    }
    case 'ADD_NOTIFICATION':
      return { ...state, notifications: [...state.notifications, action.payload] };
    case 'TYPING_STATUS':
      return {
        ...state,
        typingUsers: new Map(state.typingUsers).set(
          action.payload.username,
          action.payload.isTyping
        ),
      };
    case 'SET_RECEIVED':
      return { ...state, receivedRequests: action.payload };
    case 'SET_SENT':
      return { ...state, sentRequests: action.payload };
    case 'SET_FRIENDS':
      return { ...state, friends: action.payload };
    case 'REMOVE_FRIEND':
      return {
        ...state,
        friends: state.friends.filter(f => f.friendId !== action.payload)
      };
    case 'SET_DM_ROOMS':
      return { ...state, dmRooms: [...action.payload] };
    case 'ADD_OR_UPDATE_DM_ROOM': {
      const existingIndex = state.dmRooms.findIndex(room => room.id === action.payload.id);
      if (existingIndex >= 0) {
        const updatedRooms = [...state.dmRooms];
        updatedRooms[existingIndex] = action.payload;
        return { ...state, dmRooms: updatedRooms };
      } else {
        return { ...state, dmRooms: [...state.dmRooms, action.payload] };
      }
    }
    case 'SET_SERVER_MEMBERS':
      return {
        ...state,
        serverMembers: {
          ...state.serverMembers,
          [action.serverId]: action.payload
        }
      };
    case 'START_LOADING_SERVER_MEMBERS':
      return {
        ...state,
        loadingServerMembers: new Set(state.loadingServerMembers).add(action.payload),
      };
    case 'FINISH_LOADING_SERVER_MEMBERS': {
      const newSet = new Set(state.loadingServerMembers);
      newSet.delete(action.payload);
      return {
        ...state,
        loadingServerMembers: newSet,
      };
    }
    case 'RESET':
    return { ...initialState };
    
    default:
      return state;
  }
}

export function RealtimeProvider({ children, socket }) {
  const [state, dispatch] = useReducer(realtimeReducer, initialState);
  const [ready, setReady] = useState(false);
  const { user } = useUser();
  const username = user?.username;
  const token = user?.token;
  const subscribeFnRef = useRef(null);
  const unsubscribeFnRef = useRef(null); // ✅ 해제용 ref 추가
  const { connected, subscribe, connect, disconnect, send } = socket;
  const { toast } = useToast();
  usePing();

  const initFriendState = async () => {
    try {
      const [friendsRes, receivedRes, sentRes, onlineRes, dmRoomsRes] = await Promise.all([
        axiosInstance.get("/friends"),
        axiosInstance.get("/friends/requests/received"),
        axiosInstance.get("/friends/requests/sent"),
        axiosInstance.get("/friends/online"),
        axiosInstance.get(`/dm/rooms/${user.id}`),
      ]);
      dispatch({ type: "SET_FRIENDS", payload: friendsRes.data || [] });
      dispatch({ type: "SET_RECEIVED", payload: receivedRes.data || [] });
      dispatch({ type: "SET_SENT", payload: sentRes.data || [] });
      dispatch({ type: "SET_ONLINE_USERS", payload: onlineRes.data || [] });
      dispatch({ type: "SET_DM_ROOMS", payload: dmRoomsRes.data || [] });
      console.log("✅ 친구 상태 초기화 완료");
    } catch (err) {
      console.error("❌ 친구 상태 초기화 실패:", err);
    }
  };

  const refreshDmRooms = async () => {
    try {
      console.log("🔄 DM 목록 새로고침 시작...");
      const dmRoomsRes = await axiosInstance.get(`/dm/rooms/${user.id}`);
      dispatch({ type: "SET_DM_ROOMS", payload: dmRoomsRes.data || [] });
      console.log("✅ DM 목록 새로고침 완료:", dmRoomsRes.data);
    } catch (err) {
      console.error("❌ DM 목록 새로고침 실패:", err);
    }
  };

  const fetchAndSetServerMembers = async (serverId) => {
    try {
      const res = await axiosInstance.get(`/servers/${serverId}/members`);
      dispatch({ type: 'SET_SERVER_MEMBERS', serverId, payload: res.data });
    } catch (err) {
      console.error(`❌ 서버 멤버 갱신 실패 (serverId=${serverId}):`, err);
    }
  };

  useEffect(() => {
    if (token && user?.id) {
      console.log("🟥 RealtimeProvider Mounted");

      connect(token, () => {
        console.log("🟢 WebSocket connected → setReady(true)");
        subscribeFnRef.current = subscribeAll;
        setReady(true);
        initFriendState();
      });
    }

    return () => {
      unsubscribeFnRef.current?.();
      disconnect();
    };
  }, [token, user?.id]);

  useEffect(() => {
    if (connected && ready && subscribeFnRef.current) {
      console.log("🔄 재연결 후 수동 재구독 시도");
      subscribeFnRef.current(); // 내부에서 이전 구독을 해제함
    }
  }, [connected]);

  function subscribeAll() {
    if (!username || !user?.id) {
      console.warn("⚠️ username 또는 user.id 누락 → 구독 스킵");
      return () => {};
    }

    // ✅ 이전 구독 해제
    if (unsubscribeFnRef.current) {
      console.log("🧹 이전 구독 해제 시도...");
      unsubscribeFnRef.current();
    }

    console.log("🔔 WebSocket 구독 시작:", username);

    const subStatus = subscribe(`/user/queue/status`, ev => {
      dispatch({ type: 'USER_STATUS_CHANGE', payload: ev });

      if (ev.username !== username) {
        toast({
          title: ev.status === "ONLINE" ? "🔔 친구 접속" : "🔕 친구 퇴장",
          description: `${ev.username}님이 ${ev.status} 상태가 되었습니다.`,
        });
      }
    });

    const subBroadcast = subscribe(`/topic/status`, ev => {
      dispatch({ type: 'USER_STATUS_CHANGE', payload: ev });
    });

    const subNoti = subscribe(`/user/queue/notifications.${username}`, msg => {
      dispatch({ type: 'ADD_NOTIFICATION', payload: msg });
    });

    const subFriend = subscribe(`/user/queue/friend`, async payload => {
      try {
        const type = payload.type;

        if (["REQUEST_RECEIVED", "REQUEST_CANCELLED", "REQUEST_ACCEPTED", "REQUEST_REJECTED"].includes(type)) {
          const [friendsRes, receivedRes, sentRes, onlineRes] = await Promise.all([
            axiosInstance.get("/friends"),
            axiosInstance.get("/friends/requests/received"),
            axiosInstance.get("/friends/requests/sent"),
            axiosInstance.get("/friends/online"),
          ]);
          dispatch({ type: "SET_FRIENDS", payload: friendsRes.data || [] });
          dispatch({ type: "SET_RECEIVED", payload: receivedRes.data || [] });
          dispatch({ type: "SET_SENT", payload: sentRes.data || [] });
          dispatch({ type: "SET_ONLINE_USERS", payload: onlineRes.data || [] });
        } else if (type === "FRIEND_DELETED") {
          const friendId = payload.payload?.friendId;
          if (friendId) {
            dispatch({ type: "REMOVE_FRIEND", payload: friendId });
          } else {
            console.warn("⚠️ FRIEND_DELETED → friendId 없음:", payload);
          }
        }
      } catch (err) {
        console.error("❌ 친구 이벤트 처리 실패:", err);
      }
    });

    const subDmRestore = subscribe(`/user/queue/dm-restore`, async payload => {
      console.log("📥 DM 복구 알림 수신:", payload);
      try {
        await refreshDmRooms();
        toast({
          title: "💬 DM 복구",
          description: payload.status === "NEW" ? "새로운 DM방이 생성되었습니다." : "숨겨진 DM방이 복구되었습니다.",
        });
      } catch (err) {
        console.error("❌ DM 복구 처리 실패:", err);
      }
    });

    const subServerMemberEvent = subscribe(`/topic/server.*.members`, async payload => {
      if (!payload.serverId) return;
      await fetchAndSetServerMembers(payload.serverId);
    });

    // ✅ 해제 함수 저장
    const unsubscribe = () => {
      subStatus?.unsubscribe?.();
      subBroadcast?.unsubscribe?.();
      subNoti?.unsubscribe?.();
      subFriend?.unsubscribe?.();
      subDmRestore?.unsubscribe?.();
      subServerMemberEvent?.unsubscribe?.();
      console.log("✅ WebSocket 구독 해제 완료");
    };

    unsubscribeFnRef.current = unsubscribe;

    console.log("✅ 모든 WebSocket 구독 완료");

    return unsubscribe;
  }

  return (
    <RealtimeContext.Provider value={{
      state,
      dispatch,
      ready,
      refreshDmRooms,
      fetchAndSetServerMembers,
    }}>
      {children}
    </RealtimeContext.Provider>
  );
}

export const useRealtime = () => useContext(RealtimeContext);
