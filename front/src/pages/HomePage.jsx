// src/pages/HomePage.jsx
import { useEffect } from "react";
import { useLocation } from "react-router-dom";
export default function HomePage() {
   const location = useLocation();

   useEffect(() => {
    if (location.state?.refresh) {
      console.log("🏠 홈 새로고침 트리거됨");
      // fetchHomeData(); 필요한 경우
    }
  }, [location.state?.refresh]);
  
  return (
    <div className="flex-1 flex items-center justify-center text-white">
      Home
    </div>
  );
}
