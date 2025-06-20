package com.baidu.netdisk.preview.service;

import com.baidu.netdisk.preview.entity.File;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FileService {
    List<File> getFilesByUserIdAndFileType(Long userId, String fileType, Pageable pageable);
    List<File> getFilesByUserIdAndFileType(Long userId, String fileType);
    File getFileById(Long fileId);
    byte[] downloadFile(Long fileId);
    void setFileFavoriteStatus(Long fileId, Boolean isFavorite);
    List<File> getFavoriteFilesByUserId(Long userId, Pageable pageable);
    List<File> getFavoriteFilesByUserId(Long userId);
    List<File> searchFilesByFileName(Long userId, String fileName, Pageable pageable);
}