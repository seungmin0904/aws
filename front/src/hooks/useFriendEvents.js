// import { useEffect } from "react";
// import { useWebSocket } from "@/hooks/useWebSocket";
// import { useRealtime } from "@/context/RealtimeContext";
// import axios from "@/lib/axiosInstance";

// export default function useFriendEvents() {
//   const { subscribe, connected } = useWebSocket();
//   const { dispatch } = useRealtime();

//   useEffect(() => {
//     if (!connected) return;
  
//     const sub = subscribe("/user/queue/friend", async (payload) => {
//       if (payload.type === "REQUEST_RECEIVED") {
//         const res = await axiosInstance.get("/friends/requests/received");
//         dispatch({ type: "SET_RECEIVED", payload: res.data || [] });
//       }
//     });
  
//     return () => sub?.unsubscribe?.();
//   }, [connected]);
// }
