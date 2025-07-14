package com.example.boardapi.repository;

import com.example.boardapi.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByCodeAndActiveTrue(String code);

    List<Invite> findAllByServer_IdAndActiveTrue(Long serverId);

}
