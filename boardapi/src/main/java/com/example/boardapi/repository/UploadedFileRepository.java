package com.example.boardapi.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.boardapi.entity.UploadedFile;


public interface UploadedFileRepository extends JpaRepository<UploadedFile,Long>{
    List<UploadedFile> findByUploadDateBeforeAndDeletedFalse(LocalDateTime cutoff);
}
