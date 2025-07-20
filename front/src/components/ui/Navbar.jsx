import React from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useUser } from "@/context/UserContext";
import { useLogout } from "@/hooks/useLogout";
import "@/styles/atom-button.css";
import "@/styles/NeonButton.css";


const Navbar = () => {
  const logout = useLogout();
  const { user } = useUser();
  const isLoggedIn = !!user?.token;
  const location = useLocation();
  const navigate = useNavigate();

  const handleNavClick = (targetPath) => {
    if (location.pathname === targetPath) {
      navigate(targetPath, {
        state: { refresh: Date.now() },
        replace: true
      });
    } else {
      navigate(targetPath);
    }
  };

   return (
    <nav className="fixed top-0 w-full z-50 bg-white [#18181b] text-black  shadow-md">
  <div className="flex items-center justify-between px-6 py-4 w-full">
    {/* 왼쪽: 로고 */}
    <div onClick={() => handleNavClick("/")}>
  <Link to="/" className="neon-btn">
    <span></span>
    <span></span>
    <span></span>
    <span></span>
    Simple Board
  </Link>
</div>
    {/* 오른쪽: 메뉴 */}
    <div className="flex items-center space-x-4 text-sm">
      {/* 로그인 사용자 닉네임 표시 */}
    {isLoggedIn && user?.nickname && (
    <span className="text-gray-500"> 닉네임: {user.nickname}</span>
    )}
      <button 
      onClick={() => handleNavClick("/posts")} 
      className="hover:underline bg-transparent border-none outline-none focus:outline-none
      ">
            게시판
          </button>
      {isLoggedIn ? (
        <>
          <button onClick={() => handleNavClick("/mypage")} 
          className="hover:underline bg-transparent border-none outline-none focus:outline-none">
            마이페이지
          </button>
          <div className="atom-btn" onClick={logout}>
  <span>로그아웃</span>
  <div className="dot"></div>
</div>
        </>
      ) : (
        <>
          <button onClick={() => handleNavClick("/login")} 
          className="hover:underline bg-transparent border-none outline-none focus:outline-none">
                로그인
              </button>
          <button onClick={() => handleNavClick("/register")} 
          className="hover:underline bg-transparent border-none outline-none focus:outline-none">
                회원가입
              </button>
        </>
      )}
    </div>
  </div>
</nav>
  );
};

export default Navbar;