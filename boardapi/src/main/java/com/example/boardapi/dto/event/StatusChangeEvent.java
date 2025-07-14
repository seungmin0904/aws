package com.example.boardapi.dto.event;

import java.io.Serializable;

import com.example.boardapi.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class StatusChangeEvent implements Serializable {
    private String username;
    private UserStatus status; // "ONLINE" or "OFFLINE"
}
