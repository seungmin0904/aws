// package com.example.boardapi.websocket;

// import com.example.boardapi.entity.Member;
// import com.example.boardapi.repository.MemberRepository;
// import com.example.boardapi.security.util.JwtTokenProvider;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.server.ServerHttpRequest;
// import org.springframework.http.server.ServerHttpResponse;
// import org.springframework.stereotype.Component;
// import org.springframework.web.socket.WebSocketHandler;
// import org.springframework.web.socket.server.HandshakeInterceptor;

// import java.net.URI;
// import java.util.Arrays;
// import java.util.Map;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class JwtHandshakeInterceptor implements HandshakeInterceptor {

// private final JwtTokenProvider jwtUtil;
// private final MemberRepository memberRepository;

// @Override
// public boolean beforeHandshake(ServerHttpRequest request,
// ServerHttpResponse response,
// WebSocketHandler wsHandler,
// Map<String, Object> attributes) {
// try {
// URI uri = request.getURI();
// String query = uri.getQuery();
// if (query == null || !query.contains("token=")) {
// log.warn("Missing token");
// return false;
// }
// String token = Arrays.stream(query.split("&"))
// .filter(p -> p.startsWith("token="))
// .map(p -> p.substring("token=".length()))
// .findFirst()
// .orElse(null);

// if (token == null || !jwtUtil.validateToken(token)) {
// log.warn("❌ Invalid or missing token in handshake: {}");
// return false;
// } else {
// log.warn("토큰 정상 주입됨 {}", token);
// }

// String username = jwtUtil.validateAndGetUsername(token);
// if (username == null) {
// log.warn("❌ Username 추출 실패 (token: {})", token);
// return false;
// } else {
// log.warn("usename 추출 성공 {}", username);
// }
// String nickname = memberRepository.findByUsername(username)

// .map((Member m) -> m.getName())
// .orElse("알 수 없음");

// attributes.put("username", username);
// attributes.put("nickname", nickname);
// log.info("Handshake OK for {}", username);
// return true;
// } catch (Exception e) {
// log.error("Handshake error", e);
// return false;
// }
// }

// @Override
// public void afterHandshake(ServerHttpRequest request,
// ServerHttpResponse response,
// WebSocketHandler wsHandler,
// Exception ex) {
// // no-op
// }

// }
