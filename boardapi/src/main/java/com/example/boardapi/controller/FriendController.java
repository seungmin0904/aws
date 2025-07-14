package com.example.boardapi.controller;

import com.example.boardapi.dto.FriendDTO;
import com.example.boardapi.entity.Friend;
import com.example.boardapi.entity.FriendStatus;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.service.FriendService;
import com.example.boardapi.service.UserStatusService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserStatusService userStatusService;

    @PostMapping
    public ResponseEntity<?> requestFriend(@RequestBody FriendDTO.Request dto,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        friendService.requestFriend(principal.getMno(), dto.getTargetMemberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<?> acceptFriend(@PathVariable Long friendId,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        friendService.acceptFriend(friendId, principal.getMno());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendId}/reject")
    public ResponseEntity<?> rejectFriend(@PathVariable Long friendId,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        friendService.rejectFriend(friendId, principal.getMno());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        List<FriendDTO.SimpleResponse> result = friendService.getFriends(principal.getMno());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{targetId}")
    public ResponseEntity<?> getStatus(@PathVariable Long targetId,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        FriendStatus status = friendService.getStatus(principal.getMno(), targetId);
        return ResponseEntity.ok(new FriendDTO.StatusResponse(status));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long friendId,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        Long myId = principal.getMno();
        Friend friend = friendService.getFriendOrThrow(friendId);

        if (friend.getStatus() == FriendStatus.REQUESTED &&
                friend.getMemberA().getMno().equals(myId)) {
            friendService.cancelFriendRequest(friendId, myId);
        } else {
            friendService.deleteFriend(friendId, myId);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests/received")
    public ResponseEntity<?> getReceivedFriendRequests(@AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        List<FriendDTO.RequestResponse> list = friendService.getReceivedFriendRequests(principal.getMno());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<?> getSentFriendRequests(@AuthenticationPrincipal MemberSecurityDTO principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        List<FriendDTO.RequestResponse> list = friendService.getSentFriendRequests(principal.getMno());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlineFriends(Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        String me = principal.getName();
        List<String> onlineFriends = userStatusService.getOnlineFriendUsernames(me);
        return ResponseEntity.ok(onlineFriends);
    }
}
