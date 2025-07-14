import axios from "axios";

// ✅ 빌드 시점에 고정된 baseURL을 사용 (Vite가 import.meta.env로 주입함)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// 공통 인스턴스
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

const refreshAxios = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// 요청 인터셉터
axiosInstance.interceptors.request.use((config) => {
  // ✅ Electron 환경이라면 electronAPI를 통해 토큰 가져오기
  const token = typeof window !== "undefined"
    ? (window.electronAPI?.getToken?.() || localStorage.getItem("token"))
    : null;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

refreshAxios.interceptors.request.use((config) => {
  return config;
});

// 토큰 갱신 로직
let isRefreshing = false;
let refreshSubscribers = [];

function onRefreshed(newToken) {
  refreshSubscribers.forEach((cb) => cb(newToken));
  refreshSubscribers = [];
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb);
}

axiosInstance.interceptors.response.use(
  (res) => res,
  async (err) => {
    const originalConfig = err.config;
    originalConfig._retryCount = originalConfig._retryCount || 0;

    if (err.response?.status === 401 && originalConfig._retryCount < 1) {
      originalConfig._retryCount++;

      const refreshToken = localStorage.getItem("refresh_token");
      if (!refreshToken) {
        clearSession();
        redirectToLogin();
        return Promise.reject(err);
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((newToken) => {
            originalConfig.headers.Authorization = `Bearer ${newToken}`;
            resolve(axiosInstance(originalConfig));
          });
        });
      }

      isRefreshing = true;

      try {
        console.log("🔄 AccessToken 만료 → RefreshToken 사용 시도");
        const response = await refreshAxios.post("/auth/refresh", { refreshToken });
        const newToken = response.data.token;

        localStorage.setItem("token", newToken);
        isRefreshing = false;
        onRefreshed(newToken);

        originalConfig.headers.Authorization = `Bearer ${newToken}`;
        return axiosInstance(originalConfig);
      } catch (refreshError) {
        console.warn("❌ RefreshToken 재발급 실패");
        isRefreshing = false;
        clearSession();
        redirectToLogin();
        return Promise.reject(refreshError);
      }
    }

    if (originalConfig._retryCount >= 1) {
      console.warn("🚫 최대 재시도 횟수 초과");
      clearSession();
      redirectToLogin();
    }

    return Promise.reject(err);
  }
);

function clearSession() {
  localStorage.clear();
}

function redirectToLogin() {
  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.replace("/login");
  }
}

export default axiosInstance;
