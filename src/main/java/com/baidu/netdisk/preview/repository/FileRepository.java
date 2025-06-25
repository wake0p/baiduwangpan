package com.baidu.netdisk.preview.repository;

import com.baidu.netdisk.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserIdAndFileType(Long userId, String fileType);
    List<File> findByUserIdAndFileType(Long userId, String fileType, Pageable pageable);
    List<File> findByUserId(Long userId, Pageable pageable);
    List<File> findByUserId(Long userId);

    List<File> findByUserIdAndIsFavorite(Long userId, Boolean isFavorite, Pageable pageable);
    List<File> findByUserIdAndIsFavorite(Long userId, Boolean isFavorite);
    List<File> findByUserIdAndFileNameContaining(Long userId, String fileName, Pageable pageable);
}