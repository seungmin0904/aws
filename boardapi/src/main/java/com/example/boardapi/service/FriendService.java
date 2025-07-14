package com.example.boardapi.service;

import com.example.boardapi.dto.FriendDTO;
import com.example.boardapi.dto.event.FriendEvent;
import com.example.boardapi.entity.Friend;
import com.example.boardapi.entity.Member;
import com.example.boardapi.infra.EventPublisher;
import com.example.boardapi.entity.FriendStatus;
import com.example.boardapi.repository.FriendRepository;
import com.example.boardapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<String> getFriendUsernames(String username) {
        Long myId = memberService.getByUsername(username).getMno();
        return friendRepository.findFriendUsernamesByStatusAndMyId(FriendStatus.ACCEPTED, myId);
    }

    @Transactional
    public void requestFriend(Long myId, Long targetId) {
        if (myId.equals(targetId))
            throw new IllegalArgumentException("본인은 친구추가 불가");

        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Member you = memberRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("상대 없음"));

        Friend friend;

        Optional<Friend> existing = friendRepository.findByMemberAAndMemberB(me, you);
        if (existing.isPresent()) {
            Friend f = existing.get();
            if (f.getStatus() == FriendStatus.REJECTED) {
                f.setStatus(FriendStatus.REQUESTED);
                f.setCreatedAt(LocalDateTime.now());
                f.setMemberA(me);
                f.setMemberB(you);
                friend = f;
            } else {
                throw new IllegalStateException("이미 친구 신청 중/수락됨");
            }
        } else {
            Optional<Friend> reverse = friendRepository.findByMemberAAndMemberB(you, me);
            if (reverse.isPresent()) {
                Friend f = reverse.get();
                if (f.getStatus() == FriendStatus.REJECTED) {
                    f.setStatus(FriendStatus.REQUESTED);
                    f.setCreatedAt(LocalDateTime.now());
                    f.setMemberA(me);
                    f.setMemberB(you);
                    friend = f;
                } else {
                    throw new IllegalStateException("이미 친구 신청 중/수락됨");
                }
            } else {
                friend = Friend.builder()
                        .memberA(me)
                        .memberB(you)
                        .status(FriendStatus.REQUESTED)
                        .createdAt(LocalDateTime.now())
                        .build();
                friendRepository.save(friend);
            }
        }

        friendRepository.findByMemberAAndMemberB(you, me)
                .filter(f -> f.getStatus() == FriendStatus.REJECTED)
                .ifPresent(friendRepository::delete);

        FriendEvent toReceiver = new FriendEvent(
                "REQUEST_RECEIVED",
                targetId,
                FriendDTO.RequestResponse.from(friend));
        eventPublisher.publishFriendEvent(toReceiver, you.getUsername());

        FriendEvent toSender = new FriendEvent(
                "REQUEST_RECEIVED",
                myId,
                FriendDTO.RequestResponse.from(friend));
        eventPublisher.publishFriendEvent(toSender, me.getUsername());

    }

    @Transactional
    public void acceptFriend(Long friendId, Long myId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청 없음"));

        if (!friend.getMemberB().getMno().equals(myId))
            throw new IllegalStateException("수락 권한 없음");

        friend.setStatus(FriendStatus.ACCEPTED);
        friendRepository.save(friend);

        FriendEvent toRequester = new FriendEvent(
                "REQUEST_ACCEPTED",
                friend.getMemberA().getMno(),
                FriendDTO.RequestResponse.from(friend));
        FriendEvent toAccepter = new FriendEvent(
                "REQUEST_ACCEPTED",
                friend.getMemberB().getMno(),
                FriendDTO.RequestResponse.from(friend));

        eventPublisher.publishFriendEvent(toRequester, friend.getMemberA().getUsername());
        eventPublisher.publishFriendEvent(toAccepter, friend.getMemberB().getUsername());

        friendRepository.findByMemberAAndMemberB(friend.getMemberB(), friend.getMemberA())
                .filter(f -> f.getStatus() == FriendStatus.REJECTED)
                .ifPresent(friendRepository::delete);
    }

    public List<FriendDTO.SimpleResponse> getFriends(Long myId) {
        List<Friend> friends = friendRepository.findAcceptedFriends(FriendStatus.ACCEPTED, myId);
        return friends.stream().map(f -> FriendDTO.SimpleResponse.from(f, myId)).toList();
    }

    @Transactional
    public void rejectFriend(Long friendId, Long myId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청 없음"));

        if (!friend.getMemberB().getMno().equals(myId))
            throw new IllegalStateException("거절 권한 없음");

        friend.setStatus(FriendStatus.REJECTED);
        friendRepository.save(friend);

        FriendEvent event = new FriendEvent(
                "REQUEST_REJECTED",
                friend.getMemberA().getMno(),
                FriendDTO.RequestResponse.from(friend));
        eventPublisher.publishFriendEvent(event, friend.getMemberA().getUsername());
    }

    @Transactional
    public void cancelFriendRequest(Long friendId, Long myId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("요청 없음"));

        if (!friend.getMemberA().getMno().equals(myId))
            throw new IllegalStateException("요청 취소 권한 없음");

        Member receiver = friend.getMemberB();
        friendRepository.delete(friend);

        FriendEvent cancelEvent = new FriendEvent(
                "REQUEST_CANCELLED",
                receiver.getMno(),
                FriendDTO.RequestResponse.from(friend));
        eventPublisher.publishFriendEvent(cancelEvent, receiver.getUsername());
    }

    public FriendStatus getStatus(Long myId, Long targetId) {
        if (myId.equals(targetId))
            return FriendStatus.NONE;
        Optional<Friend> relation = friendRepository.findRelation(myId, targetId);
        return relation.map(Friend::getStatus).orElse(FriendStatus.NONE);
    }

    @Transactional
    public void deleteFriend(Long friendId, Long myId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("친구 없음"));

        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Member other;
        if (friend.getMemberA().getMno().equals(myId)) {
            other = friend.getMemberB();
        } else if (friend.getMemberB().getMno().equals(myId)) {
            other = friend.getMemberA();
        } else {
            throw new IllegalStateException("삭제 권한 없음");
        }

        friendRepository.delete(friend);

        FriendEvent toMe = new FriendEvent(
                "FRIEND_DELETED",
                me.getMno(),
                FriendDTO.RequestResponse.from(friend));
        FriendEvent toOther = new FriendEvent(
                "FRIEND_DELETED",
                other.getMno(),
                FriendDTO.RequestResponse.from(friend));

        eventPublisher.publishFriendEvent(toMe, me.getUsername());
        eventPublisher.publishFriendEvent(toOther, other.getUsername());
    }

    public List<FriendDTO.RequestResponse> getReceivedFriendRequests(Long memberId) {
        List<Friend> receivedRequests = friendRepository.findByMemberBMnoAndStatus(memberId, FriendStatus.REQUESTED);
        return receivedRequests.stream()
                .map(FriendDTO.RequestResponse::from)
                .collect(Collectors.toList());
    }

    public List<FriendDTO.RequestResponse> getSentFriendRequests(Long memberId) {
        List<Friend> sentRequests = friendRepository.findByMemberAMnoAndStatus(memberId, FriendStatus.REQUESTED);
        return sentRequests.stream()
                .map(FriendDTO.RequestResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Friend getFriendOrThrow(Long friendId) {
        return friendRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("친구 정보 없음"));
    }
}
