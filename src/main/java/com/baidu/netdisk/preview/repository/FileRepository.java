package com.baidu.netdisk.preview.repository;

import com.baidu.netdisk.entity.NetdiskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<NetdiskFile, Long> {
    List<NetdiskFile> findByUserIdAndFileType(Long userId, String fileType);
    List<NetdiskFile> findByUserIdAndFileType(Long userId, String fileType, Pageable pageable);
    List<NetdiskFile> findByUserId(Long userId, Pageable pageable);
    List<NetdiskFile> findByUserId(Long userId);

    List<NetdiskFile> findByUserIdAndIsFavorite(Long userId, Boolean isFavorite, Pageable pageable);
    List<NetdiskFile> findByUserIdAndIsFavorite(Long userId, Boolean isFavorite);
    List<NetdiskFile> findByUserIdAndFileNameContaining(Long userId, String fileName, Pageable pageable);
}