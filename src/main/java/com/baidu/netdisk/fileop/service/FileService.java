package com.baidu.netdisk.fileop.service;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.fileop.Mapper.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {
    @Autowired
    private FileMapper fileMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 计算文件MD5
    public String calculateFileMd5(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (InputStream is = file.getInputStream()) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    // 字节数组转十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // 创建文件
    public File createFile(MultipartFile file, Long folderId, String fileName,  String fileType, Boolean status,Long userId, Boolean isDeleted) {
        try {
            // 构建存储目录
            Path storageDir = Paths.get(uploadDir);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            // 生成唯一文件名
            String storedFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path storagePath = storageDir.resolve(storedFileName);

            // 保存文件
            Files.write(storagePath, file.getBytes());

            // 创建文件实体（存储相对路径）
            File newFile = new File();
            newFile.setFileName(fileName);
            newFile.setFilePath(storedFileName); // 只存储文件名，而非完整路径
            newFile.setFileSize(file.getSize());
            newFile.setFolderId(folderId);
            newFile.setUserId(userId);
            newFile.setStatus(status);
            newFile.setIsDeleted(isDeleted);
            newFile.setCreateTime(LocalDateTime.now());
            newFile.setFileType(fileType);
            newFile.setFileMd5(calculateFileMd5(file));

            // 保存到数据库
            fileMapper.insertFile(newFile);
            return newFile;
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    // 获取文件完整路径（运行时动态构建）
    public Path getFullFilePath(File file) {
        return Paths.get(uploadDir, file.getFilePath());
    }

    // 获取文件夹下的文件
    public List<File> getFilesByFolderId(Long folderId) {
        return fileMapper.getFilesByFolderId(folderId);
    }

    // 重命名文件
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

    // 删除文件夹下的文件（逻辑删除）
    public void deleteFilesByFolderId(Long folderId,Long userId) {
        fileMapper.deleteFilesByFolderId(folderId);
    }

    // 判断文件是否可预览
    public boolean canPreview(File file) {
        String fileType = file.getFileType().toLowerCase();
        return fileType.contains("office")
                || fileType.contains("image")
                || fileType.contains("video")
                || fileType.contains("audio")
                || fileType.contains("text");
    }

    // 获取文件
    public File getFileById(Long fileId) {
        return fileMapper.getFileById(fileId);
    }

    // ===================== 回收站相关方法 =====================

    // 移入回收站（批量）
    @Transactional
    public void moveToRecycleBin(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        // 标记文件为已删除（通过 Mapper 方法更新状态）
        fileMapper.updateToRecycle(fileIds, userId);
    }

    /**
     * 从回收站恢复（批量）
     */
    @Transactional
    public void restoreFromRecycleBin(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        // 恢复文件状态（通过 Mapper 方法清除删除标记）
        fileMapper.restoreFromRecycle(fileIds, userId);
    }

    /**
     * 彻底删除文件（物理删除，含数据库+文件系统）
     */
    @Transactional
    public void deletePermanently(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            File file = fileMapper.getFileById(fileId);
            if (file == null || !file.getUserId().equals(userId)) {
                continue; // 文件不存在或用户不匹配，跳过
            }
            // 1. 物理删除文件（可选：根据业务决定是否删除文件系统中的文件）
            try {
                Files.deleteIfExists(Paths.get(uploadDir, file.getFilePath()));
            } catch (IOException e) {
                // 记录日志，不中断事务
            }
            // 2. 从数据库物理删除
            fileMapper.deleteFile(fileId, userId);
        }
    }

    /**
     * 获取回收站文件列表（分页）
     */
    public List<File> getRecycleBinFiles(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return fileMapper.getRecycleBinFiles(userId, offset, pageSize);
    }

    /**
     * 获取回收站文件总数（用于分页计算）
     */
    public int getRecycleBinFileCount(Long userId) {
        return fileMapper.getRecycleBinFileCount(userId);
    }

    // ===================== 层级结构相关方法 =====================

    /**
     * 获取用户所有文件（用于层级结构）
     */
    public List<File> getAllFilesByUserId(Long userId) {
        return fileMapper.getAllFilesByUserId(userId);
    }

    /**
     * 获取指定文件夹下的所有文件（包括子文件夹中的文件）
     */
    public List<File> getAllFilesByFolderId(Long folderId, Long userId) {
        return fileMapper.getAllFilesByFolderId(folderId, userId);
    }
}