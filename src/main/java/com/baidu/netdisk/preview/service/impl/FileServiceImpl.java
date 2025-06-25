package com.baidu.netdisk.preview.service.impl;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.preview.repository.FileRepository;
import com.baidu.netdisk.preview.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;
    
    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public List<File> getFilesByUserIdAndFileType(Long userId, String fileType, Pageable pageable) {
        return fileRepository.findByUserIdAndFileType(userId, fileType, pageable);
    }

    @Override
    public List<File> getFilesByUserIdAndFileType(Long userId, String fileType) {
        return fileRepository.findByUserIdAndFileType(userId, fileType);
    }

    @Override
    public File getFileById(Long fileId) {
        Optional<File> fileOptional = fileRepository.findById(fileId);
        return fileOptional.orElse(null);
    }

    @Override
    public byte[] downloadFile(Long fileId) {
        File file = getFileById(fileId);
        if (file != null) {
            try {
                // 实际应用中从文件存储路径读取文件内容
                String filePath = file.getFilePath();
                if (filePath == null || filePath.isEmpty()) {
                    // 如果文件路径为空，使用默认路径
                    filePath = uploadDir + java.io.File.separator + file.getId() + "_" + file.getFileName();
                }
                
                java.io.File physicalFile = new java.io.File(filePath);
                if (physicalFile.exists()) {
                    return Files.readAllBytes(Paths.get(filePath));
                } else {
                    // 文件不存在，返回一个默认的占位符
                    // 根据文件类型提供不同的占位符
                    return getPlaceholderForFileType(file.getFileType());
                }
            } catch (IOException e) {
                // 处理文件读取异常
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
    
    /**
     * 根据文件类型返回适当的占位符内容
     */
    private byte[] getPlaceholderForFileType(String fileType) {
        // 这里简化处理，实际应用中可能需要更复杂的逻辑
        String placeholder = "文件无法预览";
        if ("image".equalsIgnoreCase(fileType)) {
            placeholder = "图片文件无法预览";
        } else if ("document".equalsIgnoreCase(fileType)) {
            placeholder = "文档文件无法预览";
        } else if ("video".equalsIgnoreCase(fileType)) {
            placeholder = "视频文件无法预览";
        } else if ("audio".equalsIgnoreCase(fileType)) {
            placeholder = "音频文件无法预览";
        }
        return placeholder.getBytes();
    }

    @Override
    @Transactional
    public void setFileFavoriteStatus(Long fileId, Boolean isFavorite) {
        Optional<File> fileOptional = fileRepository.findById(fileId);
        fileOptional.ifPresent(file -> {
            file.setIsFavorite(isFavorite);
            fileRepository.save(file);
        });
    }

    @Override
    public List<File> getFavoriteFilesByUserId(Long userId, Pageable pageable) {
        return fileRepository.findByUserIdAndIsFavorite(userId, true, pageable);
    }

    @Override
    public List<File> getFavoriteFilesByUserId(Long userId) {
        return fileRepository.findByUserIdAndIsFavorite(userId, true);
    }

    @Override
    public List<File> searchFilesByFileName(Long userId, String fileName, Pageable pageable) {
        return fileRepository.findByUserIdAndFileNameContaining(userId, fileName, pageable);
    }
}