package com.baidu.netdisk.preview.service.impl;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.preview.repository.FileRepository;
import com.baidu.netdisk.preview.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    // 图片文件至少应该有这么多字节
    private static final int MIN_IMAGE_FILE_SIZE = 100;
    // 常用字符编码
    private static final Charset[] COMMON_CHARSETS = {
            StandardCharsets.UTF_8,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            StandardCharsets.ISO_8859_1
    };

    @Autowired
    private FileRepository fileRepository;
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public File getFileById(Long fileId) {
        Optional<File> fileOptional = fileRepository.findById(fileId);
        return fileOptional.orElse(null);
    }
    
    @Override
    public byte[] previewFile(Long fileId) {
        File file = getFileById(fileId);
        if (file == null) {
            logger.error("预览失败：文件ID {}不存在", fileId);
            return getPlaceholderForFileType("unknown");
        }

        try {
            String filePath = uploadDir+"/"+file.getFilePath();
            logger.info("预览文件：ID={}, 路径={}, 类型={}", fileId,filePath, file.getFileType());
            
            // 检查路径是否有效
            Path path = Paths.get(filePath);
            boolean fileExists = Files.exists(path) && Files.isRegularFile(path);
            
            // 如果原始路径不存在，尝试多种备选路径
            if (!fileExists) {
                logger.error("预览失败：文件路径不存在或不是有效文件 {}", filePath);
                
                // 1. 尝试使用配置的上传目录 + 文件名作为备选路径
                String fileName = file.getFileName();
                Path alternativePath1 = Paths.get(uploadDir, fileName);
                logger.info("尝试备选路径1: {}", alternativePath1);
                
                if (Files.exists(alternativePath1) && Files.isRegularFile(alternativePath1)) {
                    logger.info("找到备选路径1文件，使用备选路径1");
                    path = alternativePath1;
                    fileExists = true;
                } else {
                    // 2. 尝试修复路径中的斜杠方向
                    String fixedPath = filePath.replace('\\', '/');
                    if (!fixedPath.equals(filePath)) {
                        Path alternativePath2 = Paths.get(fixedPath);
                        logger.info("尝试备选路径2: {}", alternativePath2);
                        
                        if (Files.exists(alternativePath2) && Files.isRegularFile(alternativePath2)) {
                            logger.info("找到备选路径2文件，使用备选路径2");
                            path = alternativePath2;
                            fileExists = true;
                        }
                    }
                    
                    // 3. 尝试反向修复斜杠
                    String fixedPath2 = filePath.replace('/', '\\');
                    if (!fileExists && !fixedPath2.equals(filePath)) {
                        Path alternativePath3 = Paths.get(fixedPath2);
                        logger.info("尝试备选路径3: {}", alternativePath3);
                        
                        if (Files.exists(alternativePath3) && Files.isRegularFile(alternativePath3)) {
                            logger.info("找到备选路径3文件，使用备选路径3");
                            path = alternativePath3;
                            fileExists = true;
                        }
                    }
                }
                
                if (!fileExists) {
                    logger.error("所有路径尝试均失败，无法找到文件");
                    return getPlaceholderForFileType(file.getFileType());
                }
            }
            
            // 检查文件大小
            long fileSize = Files.size(path);
            logger.info("文件大小: {} bytes", fileSize);
            
            // 对于图片文件，检查文件大小是否合理
            if (isImageFile(file.getFileType(), file.getFileName()) && fileSize < MIN_IMAGE_FILE_SIZE) {
                logger.error("图片文件太小，可能不是有效图片: {} bytes", fileSize);
                return getPlaceholderForFileType("image");
            }
            
            // 处理文本文件，尝试使用不同编码读取以避免乱码
            if (isTextFile(file.getFileType(), file.getFileName())) {
                logger.info("检测到文本文件，尝试多种编码读取");
                return readTextFileWithProperEncoding(path.toFile());
            }
            
            // 使用更安全的方式读取二进制文件
            if (isImageFile(file.getFileType(), file.getFileName())) {
                logger.info("检测到图片文件，使用二进制流方式读取");
                byte[] imageData = readBinaryFile(path.toFile());
                if (imageData != null && imageData.length > MIN_IMAGE_FILE_SIZE) {
                    return imageData;
                } else {
                    logger.error("读取到的图片数据过小或为空: {} bytes", 
                                imageData == null ? 0 : imageData.length);
                    return getPlaceholderForFileType("image");
                }
            }
            
            // 对于大文件，可以考虑只读取部分进行预览
            if (fileSize > 10 * 1024 * 1024) { // 10MB以上的文件
                logger.info("预览大文件：{}，大小：{} bytes", file.getFileName(), fileSize);
                // 这里可以根据文件类型决定如何处理大文件预览
                // 例如，对于文本文件可以只读取前几KB
                if (file.getFileType() != null && file.getFileType().toLowerCase().contains("text")) {
                    // 文本文件仅读取前100KB
                    byte[] buffer = new byte[102400]; // 100KB
                    try (InputStream is = Files.newInputStream(path)) {
                        int readBytes = is.read(buffer);
                        if (readBytes > 0) {
                            byte[] result = new byte[readBytes];
                            System.arraycopy(buffer, 0, result, 0, readBytes);
                            return result;
                        }
                    }
                }
            }
            
            // 读取文件内容
            logger.info("读取完整文件内容");
            byte[] data = Files.readAllBytes(path);
            logger.info("成功读取了 {} 字节的数据", data.length);
            return data;
        } catch (IOException e) {
            logger.error("预览文件时发生IO异常: {}", e.getMessage(), e);
            return getPlaceholderForFileType(file.getFileType());
        }
    }
    
    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String fileType, String fileName) {
        if (fileType != null) {
            String lowerType = fileType.toLowerCase();
            if (lowerType.contains("text") || lowerType.contains("txt") || 
                lowerType.equals("application/json") || lowerType.equals("application/xml")) {
                return true;
            }
        }
        
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            return lowerName.endsWith(".txt") || lowerName.endsWith(".csv") || 
                   lowerName.endsWith(".md") || lowerName.endsWith(".json") || 
                   lowerName.endsWith(".xml") || lowerName.endsWith(".log") ||
                   lowerName.endsWith(".ini") || lowerName.endsWith(".conf") ||
                   lowerName.endsWith(".properties");
        }
        
        return false;
    }
    
    /**
     * 使用适当的编码读取文本文件，尝试多种常用编码
     */
    private byte[] readTextFileWithProperEncoding(java.io.File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            logger.error("文本文件不存在或不是有效文件: {}", file.getAbsolutePath());
            return null;
        }
        
        // 先读取文件内容
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        
        // 检测可能的编码并转换
        String content = null;
        for (Charset charset : COMMON_CHARSETS) {
            try {
                String tempContent = new String(fileBytes, charset);
                // 简单检查是否有乱码的特征（如大量"?"或乱码字符）
                if (!containsLikelyGarbledText(tempContent)) {
                    logger.info("找到可能的正确编码: {}", charset);
                    content = tempContent;
                    break;
                }
            } catch (Exception e) {
                logger.warn("尝试使用编码 {} 读取失败: {}", charset, e.getMessage());
            }
        }
        
        // 如果所有编码都无法正确读取，使用UTF-8作为后备
        if (content == null) {
            logger.warn("无法确定正确编码，使用UTF-8作为后备");
            content = new String(fileBytes, StandardCharsets.UTF_8);
        }
        
        // 将处理后的内容转换回字节流
        return content.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 简单检查文本是否可能包含乱码
     */
    private boolean containsLikelyGarbledText(String text) {
        // 检查问号比例（大量编码不匹配通常会变成问号）
        int questionMarkCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '?' || c == '\uFFFD') {  // \uFFFD 是 Unicode 替换字符，常见于编码问题
                questionMarkCount++;
            }
        }
        
        // 如果问号比例超过一定阈值，可能是乱码
        double questionMarkRatio = (double) questionMarkCount / text.length();
        if (questionMarkRatio > 0.15) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileType, String fileName) {
        if (fileType != null && fileType.toLowerCase().contains("image")) {
            return true;
        }
        
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") || 
                   lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") || 
                   lowerFileName.endsWith(".bmp") || lowerFileName.endsWith(".webp") ||
                   lowerFileName.endsWith(".svg");
        }
        
        return false;
    }
    
    /**
     * 使用更安全的方式读取二进制文件
     */
    private byte[] readBinaryFile(java.io.File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            logger.error("文件不存在或不是有效文件: {}", file.getAbsolutePath());
            return null;
        }
        
        long fileSize = file.length();
        if (fileSize == 0) {
            logger.error("文件大小为零: {}", file.getAbsolutePath());
            return null;
        }
        
        logger.info("读取二进制文件: {}, 大小: {} bytes", file.getName(), fileSize);
        byte[] buffer = new byte[(int) fileSize];
        
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length && (numRead = is.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            
            // 确保读取了整个文件
            if (offset < buffer.length) {
                logger.warn("无法完整读取文件: {}, 预期 {} bytes, 实际读取 {} bytes", 
                           file.getName(), buffer.length, offset);
                
                // 返回实际读取的部分
                byte[] actualData = new byte[offset];
                System.arraycopy(buffer, 0, actualData, 0, offset);
                return actualData;
            }
            
            logger.info("成功读取二进制文件: {} bytes", buffer.length);
            return buffer;
        } catch (IOException e) {
            logger.error("读取二进制文件失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 根据文件类型返回适当的占位符内容
     */
    private byte[] getPlaceholderForFileType(String fileType) {
        // 这里简化处理，实际应用中可能需要更复杂的逻辑
        String placeholder = "文件无法预览";
        if (fileType == null) {
            return placeholder.getBytes();
        }
        
        fileType = fileType.toLowerCase();
        if (fileType.contains("image")) {
            placeholder = "图片文件无法预览";
        } else if (fileType.contains("document") || fileType.contains("pdf") || 
                   fileType.contains("doc") || fileType.contains("xls") || 
                   fileType.contains("ppt") || fileType.contains("text")) {
            placeholder = "文档文件无法预览";
        } else if (fileType.contains("video")) {
            placeholder = "视频文件无法预览";
        } else if (fileType.contains("audio")) {
            placeholder = "音频文件无法预览";
        }
        return placeholder.getBytes();
    }
}