package com.baidu.netdisk.service;

import com.baidu.netdisk.entity.NetdiskFile;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FileService {
    List<NetdiskFile> getFilesByUserIdAndFileType(Long userId, String fileType, Pageable pageable);
    List<NetdiskFile> getFilesByUserIdAndFileType(Long userId, String fileType);
    NetdiskFile getFileById(Long fileId);
    byte[] downloadFile(Long fileId);
    void setFileFavoriteStatus(Long fileId, Boolean isFavorite);
    List<NetdiskFile> getFavoriteFilesByUserId(Long userId, Pageable pageable);
    List<NetdiskFile> getFavoriteFilesByUserId(Long userId);
    List<NetdiskFile> searchFilesByFileName(Long userId, String fileName, Pageable pageable);
}