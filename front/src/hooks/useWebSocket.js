import { useRef, useState, useCallback, useEffect } from 'react';
import Stomp from 'stompjs';
import refreshAxios from '@/lib/axiosInstance';

export const useWebSocket = (token) => {
  const stompRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [reconnectTrigger, setReconnectTrigger] = useState(0);
  const tokenRef = useRef(token);
  const reconnectAttempt = useRef(0);
  const reconnectTimer = useRef(null);
  const subscriptionsRef = useRef([]);
  const maxFailedAttempts = 5;
  const maxTotalRetryDuration = 36000;
  const reconnectFailureCount = useRef(0);
  const firstFailureTime = useRef(null);

  useEffect(() => {
    tokenRef.current = token;
  }, [token]);

  const hardDisconnect = () => {
    console.log("🛑 hardDisconnect 호출됨");
    if (stompRef.current) {
      try {
        if (stompRef.current.connected) {
          console.log("🔌 STOMP 연결 해제 시도 중...");
          stompRef.current.disconnect(() => {
            console.log("🔌 STOMP disconnected (정상)");
          });
        } else if (stompRef.current.ws?.readyState !== WebSocket.CLOSED) {
          console.log("❌ WebSocket 상태 비정상 – 강제 종료 시도");
          stompRef.current.ws.close();
        }
      } catch (e) {
        console.warn("⚠️ Disconnect error", e);
      }
    }
    stompRef.current = null;
    subscriptionsRef.current = [];
    setConnected(false);
    console.log("🧹 stompRef/subscriptions 초기화 완료");
  };

  const attemptRefreshToken = async () => {
    try {
      const refreshToken = localStorage.getItem("refresh_token");
      const res = await refreshAxios.post('/auth/refresh', { refreshToken });
      const newToken = res.data.token;

      if (newToken) {
        localStorage.setItem("token", newToken);
        tokenRef.current = newToken;
        return newToken;
      }
    } catch (err) {
      console.error("❌ Token refresh failed", err);
      localStorage.clear();
      window.location.href = "/login";
    }
    return null;
  };

  const connect = useCallback(async (tokenArg, callback) => {
    const authToken = tokenArg || tokenRef.current;
    if (!authToken) {
      console.warn("❌ Token 없음 – 연결 중단");
      return;
    }

    console.log("🌐 WebSocket 연결 시도 시작");

    const socket = new WebSocket(import.meta.env.VITE_WS_URL);
    const client = Stomp.over(socket);
    client.debug = () => {};

    // stomp 연결 감지 ping 
    client.heartbeat.outgoing = 10000; // 클라이언트 → 서버
    client.heartbeat.incoming = 10000; // 서버 → 클라이언트

    if (stompRef.current) {
      try {
        console.log("🧹 이전 STOMP 인스턴스 정리 중...");
        stompRef.current.disconnect();
      } catch (e) {
        console.warn("⚠️ Disconnect error during cleanup", e);
      }
    }

    stompRef.current = client;

    client.onWebSocketError = (e) => {
      console.error("❌ WebSocket Error 발생", e);
      hardDisconnect();
      setReconnectTrigger(prev => prev + 1);
    };

    client.onWebSocketClose = () => {
      console.warn("🔌 WebSocket Closed 이벤트 발생");
      hardDisconnect();
      setReconnectTrigger(prev => prev + 1);
    };

    client.connect(
      { Authorization: "Bearer " + authToken },
      () => {
        console.log("✅ STOMP CONNECTED");
        setConnected(true);

        reconnectAttempt.current = 0;
        reconnectTimer.current = null;
        reconnectFailureCount.current = 0;
        firstFailureTime.current = null;

        console.log("🔁 재시도 관련 변수 초기화 완료");

        try {
          callback?.();
          console.log("📞 onConnect, callback 실행 완료");
        } catch (e) {
          console.error("❌ callback 오류", e);
        }

        for (const { topic, callback } of subscriptionsRef.current) {
  try {
    const isReady = client.connected && client.ws?.readyState === WebSocket.OPEN;

    if (!isReady) {
      console.warn(`⛔ 구독 스킵: ${topic} (WebSocket not ready)`);
      continue;
    }

    client.subscribe(topic, msg => {
      callback(JSON.parse(msg.body));
    });
    console.log(`📡 재구독 완료: ${topic}`);
  } catch (e) {
    console.error(`❌ 재구독 실패: ${topic}`, e);
  }
}
      },
      async (err) => {
        const msg = err?.headers?.message || "";
        console.warn("❌ STOMP connect error:", msg);

        if (reconnectFailureCount.current === 0) {
          firstFailureTime.current = Date.now();
        }
        reconnectFailureCount.current++;
        const timeElapsed = Date.now() - firstFailureTime.current;

        console.log(`📉 실패 누적 카운트: ${reconnectFailureCount.current}`);
        console.log(`⏱️ 경과 시간(ms): ${timeElapsed}`);

        if (msg.includes("Invalid JWT token")) {
          console.log("🔐 JWT 갱신 시도");
          const newToken = await attemptRefreshToken();
        
          if (newToken) {
            console.log("🔄 토큰 갱신 성공 – 재연결 시도");
            connect(newToken);
            return;
          } else {
            alert("세션 만료 or 인증 실패");
            localStorage.clear();
            window.location.href = "/login";
            return;
          }
        }
        
        if (msg.includes("Unauthorized")) {
          alert("세션 만료 or 인증 실패");
          localStorage.clear();
          window.location.href = "/login";
          return;
        }

        if (reconnectFailureCount.current >= maxFailedAttempts || timeElapsed >= maxTotalRetryDuration) {
          alert("연결 실패. 로그인 페이지로 이동");
          localStorage.clear();
          window.location.href = "/login";
          return;
        }

        hardDisconnect();
        setReconnectTrigger(prev => prev + 1);
      }
    );
  }, []);

  useEffect(() => {
    if (!tokenRef.current || reconnectTimer.current) return;

    const delay = Math.min(5000 * 2 ** reconnectAttempt.current, 30000);
    console.warn(`⏳ ${reconnectAttempt.current}회차 재연결 시도 예정 – ${delay / 1000}s 후`);

    reconnectTimer.current = setTimeout(() => {
      reconnectTimer.current = null;
      reconnectAttempt.current += 1;
      console.log(`🚀 [RETRY ${reconnectAttempt.current}] 연결 재시도 시작`);
      connect(tokenRef.current);
    }, delay);

    return () => {
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current);
        reconnectTimer.current = null;
      }
    };
  }, [reconnectTrigger]);

  const disconnect = useCallback(() => {
    hardDisconnect();
    reconnectAttempt.current = 0;
  }, []);

  const subscribe = useCallback((topic, callback) => {
    const existing = subscriptionsRef.current.find(sub => sub.topic === topic);
    if (existing) {
      console.warn(`⚠️ Already subscribed to ${topic}`);
      return {
        unsubscribe: () => {
          const client = stompRef.current;
          const wsReady = client?.ws?.readyState === WebSocket.OPEN;

          if (client?.connected && wsReady) {
            try {
              client.unsubscribe(topic);
              subscriptionsRef.current = subscriptionsRef.current.filter(sub => sub.topic !== topic);
              console.log(`🧹 구독 해제 완료: ${topic}`);
            } catch (e) {
              console.warn(`❌ unsubscribe 실패: ${topic}`, e);
            }
          } else {
            console.warn(`❌ WebSocket not ready. Cannot unsubscribe ${topic}`);
          }
        }
      };
    }

    subscriptionsRef.current.push({ topic, callback });

    const client = stompRef.current;
    const wsReady = client?.ws?.readyState === WebSocket.OPEN;

    if (!client || !client.connected || !wsReady) {
      console.warn(`⛔ Cannot subscribe to ${topic} – WebSocket not fully open`);
      return {
        unsubscribe: () => {
          console.warn(`⛔ Dummy unsubscribe for ${topic} (client not ready)`);
        }
      };
    }

    try {
      const subscription = client.subscribe(topic, msg => {
        console.log("🛬 수신 원본 메시지", msg.body);
        callback(JSON.parse(msg.body));
      });

      return {
        unsubscribe: () => {
          try {
            if (client.connected && wsReady) {
              subscription.unsubscribe();
              subscriptionsRef.current = subscriptionsRef.current.filter(sub => sub.topic !== topic);
              console.log(`🧹 구독 해제 완료: ${topic}`);
            } 
          } catch (err) {
            console.warn(`❌ unsubscribe 실패: ${topic}`, err);
          }
        }
      };
    } catch (e) {
      console.error(`❌ Error subscribing to ${topic}:`, e);
      return {
        unsubscribe: () => {
          console.warn(`⛔ Dummy unsubscribe after subscribe error for ${topic}`);
        }
      };
    }
  }, []);

  const send = useCallback((destination, body) => {
    const socketReady = stompRef.current?.ws?.readyState === WebSocket.OPEN;
    console.log("📤 메시지 전송 시도", { connected, socketReady });

    if (stompRef.current && connected && socketReady) {
      stompRef.current.send(destination, {}, JSON.stringify(body));
      console.log("📤 메시지 전송 완료", destination);
    } else {
      console.warn("❌ 메시지 전송 실패 – WebSocket 미연결 상태");
    }
  }, [connected]);

  return {
    connected,
    subscribe,
    send,
    connect,
    disconnect,
  };
};