package com.example.boardapi.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {
    private final String username;
    private final String sessionId;

    public StompPrincipal(String username, String sessionId) {
        this.username = username;
        this.sessionId = sessionId;
    }

    @Override
    public String getName() {
        return username; // 🔥 여기서 username만 반환
    }

    public String getSessionId() {
        return sessionId;
    }
}