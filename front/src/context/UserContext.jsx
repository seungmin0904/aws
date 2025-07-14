import { createContext, useContext, useEffect, useState } from "react";

export const UserContext = createContext({
  user: null,
  setUser: () => {},
});

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  useEffect(() => {
    try {
      const isElectron = typeof window !== 'undefined' && window.electronAPI?.isElectron === true;

      const token = isElectron
        ? window.electronAPI.getToken?.()
        : localStorage.getItem("token");

      const username = isElectron
        ? window.electronAPI.getUsername?.()
        : localStorage.getItem("username");

      const name = isElectron
        ? window.electronAPI.getName?.()
        : localStorage.getItem("name");

      if (token && username && name) {
        setUser({ token, username, name });
      }
    } catch (e) {
      console.error("🔒 사용자 정보 로드 실패", e);
    }
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);
