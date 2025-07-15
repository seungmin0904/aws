import { useEffect, useRef, useState } from 'react';
import io from 'socket.io-client';
import * as mediasoupClient from 'mediasoup-client';

const SERVER_URL = 'https://serverpro.kro.kr:4000';

export default function useMediasoupClient(userId, nickname) {
  const socketRef = useRef(null);
  const deviceRef = useRef(null);
  const sendTransportRef = useRef(null);
  const recvTransportRef = useRef(null);
  const producerRef = useRef(null);
  const currentChannelIdRef = useRef(null);
  const participantsRef = useRef(new Map());
  const consumerRefs = useRef([]);
  const streamRef = useRef(null);
  const micIntervalRef = useRef(null);
  const [micVolume, setMicVolume] = useState(0); // 0~100
const [speakingUserIds, setSpeakingUserIds] = useState(new Set());
  const [voiceParticipantsMap, setVoiceParticipantsMap] = useState(new Map());

  const iceServers = [
    {
      urls: "turn:3.35.114.7:3478",
      username: "testuser",
      credential: "testpass"
    }
  ];
  
  useEffect(() => {
     if (!userId || !nickname) return;
    socketRef.current = io(SERVER_URL, {
  path: '/socket.io',
  transports: ['websocket'],
  secure: true,
});

    socketRef.current.on('connect', async () => {
      console.log('✅ Connected to mediasoup server');
      console.log("📌 register emit:", { userId, nickname });
      socketRef.current.emit('register', { userId, nickname });

      const device = new mediasoupClient.Device();
      deviceRef.current = device;

      socketRef.current.emit('getRtpCapabilities', async (rtpCapabilities) => {
        console.log("🎯 RTP Capabilities:", rtpCapabilities.codecs.map(c => c.mimeType));
        await device.load({ routerRtpCapabilities: rtpCapabilities });
        console.log('📡 Device loaded');
      });
    });

    socketRef.current.on('voiceParticipantsUpdate', ({ channelId, participants }) => {
      participantsRef.current.set(channelId, participants);
      setVoiceParticipantsMap(new Map(participantsRef.current));
    });

    socketRef.current.on('newProducer', async ({ producerId }) => {
      console.log("🎧 New producer detected:", producerId);
      await consumeSpecificAudio(producerId);
    });

    return () => socketRef.current.disconnect();
  }, [userId,nickname]);

  const joinVoiceChannel = async (newChannelId) => {
    if (currentChannelIdRef.current && currentChannelIdRef.current !== newChannelId) {
      // 기존 채널 먼저 정리
      await leaveVoiceChannel();
    }
  
    // 새 채널 조인
    socketRef.current.emit('joinVoiceChannel', { channelId: newChannelId });
    currentChannelIdRef.current = newChannelId;
  };

  const leaveVoiceChannel = async () => {
    if (currentChannelIdRef.current) {
      socketRef.current.emit('leaveVoiceChannel', { channelId: currentChannelIdRef.current });
      currentChannelIdRef.current = null;
    }
  
    try {
      producerRef.current?.close();
    } catch (e) {}
    producerRef.current = null;
  
    try {
      sendTransportRef.current?.close();
    } catch (e) {}
    sendTransportRef.current = null;
  
    try {
      recvTransportRef.current?.close();
    } catch (e) {}
    recvTransportRef.current = null;
  
    consumerRefs.current.forEach((c) => {
      try { c.close(); } catch {}
    });
    consumerRefs.current = [];
  
    // ✅ 마이크 측정 타이머 정리
    if (micIntervalRef.current) {
      clearInterval(micIntervalRef.current);
      micIntervalRef.current = null;
    }
  
    // stream 정리
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
  
    // DOM 오디오 제거
    document.querySelectorAll("audio").forEach((audio) => {
      if (audio.srcObject instanceof MediaStream) {
        audio.pause();
        audio.srcObject = null;
        audio.remove();
      }
    });
  };

  const createSendTransport = async () => {
    return new Promise((resolve, reject) => {
      socketRef.current.emit('createWebRtcTransport', { direction: 'send' }, async (params) => {
        console.log("🚚 createWebRtcTransport 응답:", params);
        if (params.error) {
          console.error("❌ Transport 생성 실패:", params.error);
          return reject(params.error);
        }
      
        try {
          const transport = deviceRef.current.createSendTransport({
            ...params,
            iceServers: iceServers
          });
          sendTransportRef.current = transport;

          transport.on('connect', ({ dtlsParameters }, callback, errback) => {
            socketRef.current.emit('connectTransport', { transportId: transport.id, dtlsParameters }, (res) => {
              res.error ? errback(res.error) : callback();
            });
          });

          transport.on('produce', ({ kind, rtpParameters }, callback, errback) => {
            socketRef.current.emit('produce', { transportId: transport.id, kind, rtpParameters }, ({ id, error }) => {
              error ? errback(error) : callback({ id });
            });
          });

          resolve();
        } catch (err) {
          reject(err);
        }
      });
    });
  };

  const sendAudio = async () => {
    console.log("🎤 sendAudio called");
  
    if (!sendTransportRef.current) {
      console.warn("❌ sendTransportRef is null");
      return;
    }
  
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false
        }
      });
      const track = stream.getAudioTracks()[0];
      console.log("🎙️ Got local audio track:", track.label);
  
      console.log("✅ sendTransport connectionState:", sendTransportRef.current.connectionState);
      console.log("✅ track.readyState:", track.readyState);
      console.log("✅ track.enabled:", track.enabled);
      console.log("✅ track.muted:", track.muted);
  
      sendTransportRef.current.on('connectionstatechange', (state) => {
        console.log(`🚩 sendTransport connectionstatechange: ${state}`);
      });
  
      const audioContext = new (window.AudioContext || window.webkitAudioContext)();
      const source = audioContext.createMediaStreamSource(stream);
      const analyser = audioContext.createAnalyser();
      source.connect(analyser);
      const dataArray = new Uint8Array(analyser.frequencyBinCount);
  
      // 이전 타이머 제거
      if (micIntervalRef.current) {
        clearInterval(micIntervalRef.current);
        micIntervalRef.current = null;
      }
  
      // 마이크 레벨 측정 시작
      micIntervalRef.current = setInterval(() => {
        analyser.getByteFrequencyData(dataArray);
        const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;
        setMicVolume(avg); //  내 마이크 게이지용

        const threshold = 10; // 말하는 기준 볼륨
      
        // 자신을 말하는 사람으로 등록
        if (avg > threshold) {
          setSpeakingUserIds((prev) => {
            const newSet = new Set(prev);
            newSet.add(userId);
            return newSet;
          });
        } else {
          setSpeakingUserIds((prev) => {
            if (prev.has(userId)) {
              const newSet = new Set(prev);
              newSet.delete(userId);
              return newSet;
            }
            return prev;
          });
        }
        console.log("🎙️ Mic volume level:", Math.round(avg));
      }, 500);
  
      await sendTransportRef.current.produce({ track });
      console.log("📤 Audio produced successfully");
    } catch (err) {
      console.error("❌ sendAudio error:", err);
    }
  };

  const createRecvTransport = async () => {
    return new Promise((resolve, reject) => {
      socketRef.current.emit('createWebRtcTransport', { direction: 'recv' }, async (params) => {
        try {
          const transport = deviceRef.current.createRecvTransport({
            ...params,
            iceServers: iceServers
          });
          recvTransportRef.current = transport;

          transport.on('connect', ({ dtlsParameters }, callback, errback) => {
            socketRef.current.emit('connectTransport', { transportId: transport.id, dtlsParameters }, (res) => {
              res.error ? errback(res.error) : callback();
            });
          });

          socketRef.current.emit('getProducers', async (producerIds) => {
            for (const producerId of producerIds) {
              console.log("📦 Found existing producer:", producerId);
              await consumeSpecificAudio(producerId);
            }
          });

          resolve();
        } catch (err) {
          reject(err);
        }
      });
    });
  };

  const consumeSpecificAudio = async (producerId) => {
    if (!recvTransportRef.current) {
      console.warn("⚠️ recvTransportRef is null at consume time");
      return;
    }

    console.log("🔄 Attempting to consume audio from producer:", producerId);

    socketRef.current.emit(
      'consume',
      {
        transportId: recvTransportRef.current.id,
        producerId,
        rtpCapabilities: deviceRef.current.rtpCapabilities,
      },
      async (params) => {
        if (params.error) return console.error("❌ consume error:", params.error);

        const { id, kind, rtpParameters } = params;

        console.log("🎧 Consumed codec mimeType:", rtpParameters.codecs?.[0]?.mimeType);

        try {
          const consumer = await recvTransportRef.current.consume({
            id,
            producerId,
            kind,
            rtpParameters,
          });
          consumerRefs.current.push(consumer);

          console.log("👂 consumer.track:", consumer.track);
          console.log('👂 consumer.track.muted:', consumer.track.muted);
          console.log("🎤 consumer.track.enabled:", consumer.track.enabled);
          console.log('👂 consumer.rtpParameters:', consumer.rtpParameters);

          const stream = new MediaStream();
          stream.addTrack(consumer.track);

          console.log("📦 MediaStream tracks:", stream.getTracks());
          console.log("🎧 stream.getAudioTracks():", stream.getAudioTracks());

          const audioContext = new (window.AudioContext || window.webkitAudioContext)();
          console.log("🔊 AudioContext state (초기):", audioContext.state);

          audioContext.onstatechange = () => {
            console.log("📻 AudioContext state changed to:", audioContext.state);
          };

          try {
            await audioContext.resume();
            console.log("🟢 AudioContext resumed");
          } catch (e) {
            console.warn("❌ AudioContext resume 실패:", e);
          }

          const audio = new Audio();
          audio.srcObject = stream;
          audio.autoplay = true;
          audio.muted = false;
          audio.volume = 0.8;

          document.body.appendChild(audio);

          audio.addEventListener("playing", () => {
            console.log("🔈 actually playing");
          });

          audio.addEventListener("error", (e) => {
            console.warn("❗ Audio playback error:", e);
          });

          console.log("🔊 Audio element 상태:", audio);

          try {
            await audio.play();
            console.log("🔊 Audio playback started for producer:", producerId);
          } catch (err) {
            console.warn("🔇 Audio playback failed:", err);
          }
        } catch (err) {
          console.error("❌ Failed to create consumer or audio:", err);
        }
      }
    );
  };

  return {
    createSendTransport,
    sendAudio,
    createRecvTransport,
    consumeSpecificAudio,
    joinVoiceChannel,
    leaveVoiceChannel,
    voiceParticipantsMap,
    speakingUserIds,
    micVolume,
  };
}
