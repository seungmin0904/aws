// board-frontend/src/components/UserStatus/OnlineUsers.jsx
// import { useEffect, useState } from 'react';
// import { useUser } from "@/context/UserContext";
// import { useWebSocket } from '../../hooks/useWebSocket';

// export default function OnlineUsers() {
//   const { user } = useUser();
//   const { subscribe } = useWebSocket(user?.token);
//   const [onlineUsers, setOnlineUsers] = useState(new Set());

//   useEffect(() => {
//   if (!subscribe) return;
//   const unsubscribe = subscribe('/topic/online-users', (msg) => {
//     console.log("Received online-users", msg);
//     setOnlineUsers(new Set(msg));
//   });
//   return () => unsubscribe && unsubscribe();
// }, [subscribe]);

//   return (
//     <div className="bg-white rounded-lg shadow p-4">
//       <h3 className="text-lg font-semibold mb-2">온라인 사용자</h3>
//       <div className="space-y-2">
//         {Array.from(onlineUsers).map((username) => (
//           <div key={username} className="flex items-center space-x-2">
//             <div className="w-2 h-2 bg-green-500 rounded-full"></div>
//             <span>{username}</span>
//           </div>
//         ))}
//         {onlineUsers.size === 0 && (
//           <div className="text-gray-500 text-sm">온라인 사용자가 없습니다</div>
//         )}
//       </div>
//     </div>
//   );
// }