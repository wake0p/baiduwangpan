package com.baidu.netdisk.fileop.service;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.fileop.Mapper.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Autowired
    private FileMapper fileMapper;

    public List<File> getFilesByFolderId(Long folderId) {
        return fileMapper.getFilesByFolderId(folderId);
    }

    public boolean renameFile(Long fileId, String newName) {
        File file = fileMapper.getFileById(fileId);
        if (file != null) {
            Long folderId = file.getFolderId();
            File existingFile = fileMapper.getFileByNameAndFolderId(newName, folderId);
            if (existingFile == null) {
                file.setFileName(newName);
                fileMapper.updateFile(file);
                return true;
            }
        }
        return false;
    }

    public void deleteFilesByFolderId(Long folderId) {
        fileMapper.deleteFilesByFolderId(folderId);
    }

    public boolean canPreview(File file) {
        String fileType = file.getFileType().toLowerCase();
        return fileType.contains("office")
                || fileType.contains("image")
                || fileType.contains("video")
                || fileType.contains("audio")
                || fileType.contains("text");
    }

    public File getFileById(Long fileId) {
        return fileMapper.getFileById(fileId);
    }

    // ===================== 回收站相关方法 =====================

    @Transactional
    public void moveToRecycleBin(List<Long> fileIds, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (Long fileId : fileIds) {
            File file = fileMapper.getFileById(fileId);
            if (file != null) {
                file.setIsDeleted(1);
                file.setDeletedTime(now);
                file.setDeletedBy(userId);
                fileMapper.updateFile(file);
            }
        }
    }

    @Transactional
    public void restoreFromRecycleBin(List<Long> fileIds, Long userId) {
        for (Long fileId : fileIds) {
            File file = fileMapper.getFileById(fileId);
            if (file != null && file.getIsDeleted() == 1) {
                file.setIsDeleted(0);
                file.setDeletedTime(null);
                file.setDeletedBy(null);
                fileMapper.updateFile(file);
            }
        }
    }

    // 新增：按文件夹ID恢复文件（适配递归恢复场景）
    @Transactional
    public void restoreFilesByFolderId(Long folderId, Long userId) {
        List<File> files = fileMapper.getFilesByFolderId(folderId);
        for (File file : files) {
            if (file.getIsDeleted() == 1) {
                file.setIsDeleted(0);
                file.setDeletedTime(null);
                file.setDeletedBy(null);
                fileMapper.updateFile(file);
            }
        }
    }

    // 新增：按文件夹ID移入回收站（批量处理文件夹下文件）
    @Transactional
    public void moveFilesToRecycleBinByFolderId(Long folderId, Long userId) {
        List<File> files = fileMapper.getFilesByFolderId(folderId);
        List<Long> fileIds = files.stream()
                .map(File::getId)
                .collect(Collectors.toList());
        moveToRecycleBin(fileIds, userId);
    }

    @Transactional
    public void deletePermanently(List<Long> fileIds, Long userId) {
        for (Long fileId : fileIds) {
            File file = fileMapper.getFileById(fileId);
            if (file != null && file.getIsDeleted() == 1) {
                fileMapper.deleteFile(fileId);
            }
        }
    }

    public List<File> getRecycleBinFiles(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return fileMapper.getRecycleBinFiles(userId, offset, pageSize);
    }
}