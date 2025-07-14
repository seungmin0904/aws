package com.example.boardapi.dto.event;

public class DmRestoreEvent {

    private final String username;
    private final Long roomId;

    public DmRestoreEvent(String username, Long roomId) {
        this.username = username;
        this.roomId = roomId;
    }

    public String getUsername() {
        return username;
    }

    public Long getRoomId() {
        return roomId;
    }
}
