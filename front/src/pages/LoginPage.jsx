import { useForm } from "react-hook-form";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/hooks/use-toast";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import FindAccountModal from "@/components/ui/FindAccountModal";
import axios from "axios";

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
          username: data.username, // 이메일
          password: data.password,
        },
        {
          headers: {
            "Content-Type": "application/json",
          },
          withCredentials: true, // 쿠키 인증이나 CORS 정책 대응시 필요
        }
      );

      const result = response.data;

      if (result.token && result.username && result.name) {
        localStorage.setItem("token", result.token);
        localStorage.setItem("username", result.username); // 이메일
        localStorage.setItem("name", result.name);         // 닉네임
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
    <div className="flex min-h-screen items-start justify-center text-white bg-gray-50 dark:bg-[#18181b]">
      <div className="mt-32">
        <div className="flex flex-col items-center mb-10">
          <h1 className="text-3xl font-bold tracking-tight mb-1 text-gray-900 dark:text-white">
            Simple Board
          </h1>
          <p className="text-gray-500 dark:text-gray-300 text-base">
            누구나 쉽고 빠르게 글을 남기는 공간
          </p>
        </div>
        <div className="w-[400px] py-10 px-6 border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 rounded-2xl shadow-xl">
          <h2 className="text-2xl font-bold mb-6 text-center text-gray-900 dark:text-white">
            로그인
          </h2>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              placeholder="이메일"
              type="email"
              autoComplete="username"
              {...register("username", { required: true })}
              className="h-12 text-lg placeholder:text-gray-400 placeholder:opacity-80"
            />
            <Input
              placeholder="비밀번호"
              type="password"
              autoComplete="current-password"
              {...register("password", { required: true })}
              className="h-12 text-lg placeholder:text-gray-400 placeholder:opacity-80"
            />
            <Button type="submit" className="w-full h-12 text-lg mt-2 rounded-xl bg-[#4EA685] text-white hover:bg-[#3f8f71] ">
              로그인
            </Button>
          </form>
          <div className="flex justify-between mt-5 text-sm">
            <button
              className="px-3 py-1 rounded bg-white hover:bg-blue-100 text-blue-500 dark:bg-zinc-900 dark:hover:bg-zinc-700 dark:text-blue-400"
              onClick={() => setFindMode("id")}
              type="button"
            >
              아이디 찾기
            </button>
            <button
              className="px-3 py-1 rounded bg-white hover:bg-blue-100 text-blue-500 dark:bg-zinc-900 dark:hover:bg-zinc-700 dark:text-blue-400"
              onClick={() => setFindMode("pw")}
              type="button"
            >
              비밀번호 찾기
            </button>
          </div>
          <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-300">
            아직 회원이 아니신가요?{" "}
            <button
              className="px-3 py-1 rounded bg-white hover:bg-blue-100 text-blue-500 dark:bg-zinc-900 dark:hover:bg-zinc-700 dark:text-blue-400"
              onClick={() => navigate("/register")}
              type="button"
            >
              회원가입
            </button>
          </div>
          {findMode && (
            <FindAccountModal mode={findMode} onClose={() => setFindMode(null)} />
          )}
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
