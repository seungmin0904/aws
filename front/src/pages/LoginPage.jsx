import { useForm } from "react-hook-form";
import { useToast } from "@/hooks/use-toast";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import FindAccountModal from "@/components/ui/FindAccountModal";
import axios from "axios";
import "@/styles/login.css";

const LoginPage = ({ onLogin }) => {
  const { register, handleSubmit } = useForm();
  const { toast } = useToast();
  const navigate = useNavigate();
  const [findMode, setFindMode] = useState(null);

  const onSubmit = async (data) => {
    try {
      const response = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/members/login`,
        {
          username: data.username,
          password: data.password,
        },
        {
          headers: {
            "Content-Type": "application/json",
          },
          withCredentials: true,
        }
      );

      const result = response.data;

      if (result.token && result.username && result.name) {
        localStorage.setItem("token", result.token);
        localStorage.setItem("username", result.username);
        localStorage.setItem("name", result.name);
        localStorage.setItem("refresh_token", result.refreshToken);

        toast({
          title: "로그인 성공 🎉",
          description: `${result.name}님 환영합니다!`,
        });

        onLogin(result.token);
        navigate("/");
      } else {
        throw new Error("서버가 사용자 정보를 반환하지 않았습니다.");
      }
    } catch (error) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        toast({
          title: "인증 오류",
          description: "이메일 또는 비밀번호가 올바르지 않습니다.",
          variant: "destructive",
        });
      } else if (error.response?.status >= 500) {
        toast({
          title: "서버 점검 중",
          description: "",
          variant: "destructive",
        });
      } else {
        toast({
          title: "로그인 실패",
          description: error.message || "예상치 못한 오류가 발생했습니다.",
          variant: "destructive",
        });
      }

      onLogin(null);
      console.error("로그인 에러:", error);
    }
  };

  return (
    <div className="w-screen h-screen flex items-center justify-center bg-white relative overflow-hidden">
      {/* 우측 그라데이션 배경 */}
      <div className="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-[#fbe6ff] to-white z-0" />

      {/* 로그인 박스 */}
      <div className="relative z-10">
        <div className="screen shadow-2xl rounded-xl overflow-hidden">
          <div className="screen__content">
            <form onSubmit={handleSubmit(onSubmit)} className="login">
              <div className="login__field">
                <i className="login__icon fas fa-user"></i>
                <input
                  type="email"
                  placeholder="username"
                  className="login__input"
                  autoComplete="username"
                  {...register("username", { required: true })}
                />
              </div>
              <div className="login__field">
                <i className="login__icon fas fa-lock"></i>
                <input
                  type="password"
                  placeholder="password"
                  className="login__input"
                  autoComplete="current-password"
                  {...register("password", { required: true })}
                />
              </div>
              <button type="submit" className="button login__submit">
                <span className="button__text">Login now</span>
                <i className="button__icon fas fa-chevron-right"></i>
              </button>
            </form>
            
            <div className="mt-24 text-center text-sm text-white">
              아직 회원이 아니신가요?{" "}
              <button
                className="underline hover:text-gray-200"
                onClick={() => navigate("/register")}
                type="button"
              >
                회원가입
              </button>
            </div>

            <div className="flex justify-center gap-20 mt-6 text-sm text-white">
              <button
                className="hover:underline"
                onClick={() => setFindMode("id")}
                type="button"
              >
                아이디 찾기
              </button>
              <button
                className="hover:underline"
                onClick={() => setFindMode("pw")}
                type="button"
              >
                비밀번호 찾기
              </button>
            </div>

            {findMode && (
              <FindAccountModal mode={findMode} onClose={() => setFindMode(null)} />
            )}
          </div>

          {/* 배경 도형 */}
          <div className="screen__background">
            <span className="screen__background__shape screen__background__shape4"></span>
            <span className="screen__background__shape screen__background__shape3"></span>
            <span className="screen__background__shape screen__background__shape2"></span>
            <span className="screen__background__shape screen__background__shape1"></span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
