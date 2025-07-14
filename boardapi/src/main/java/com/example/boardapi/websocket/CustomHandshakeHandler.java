package com.example.boardapi.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String username = (String) attributes.get("username");
        if (username == null) {
            log.warn("No username in attributes");
            return null;
        }
        return () -> username;
    }

}
