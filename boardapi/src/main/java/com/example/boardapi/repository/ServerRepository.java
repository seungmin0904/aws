package com.example.boardapi.repository;

import com.example.boardapi.entity.Server;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findByNameContainingIgnoreCase(String keyword);

}
