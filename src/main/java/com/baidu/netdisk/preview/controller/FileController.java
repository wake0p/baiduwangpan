package com.baidu.netdisk.preview.controller;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.preview.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@Controller
@RequestMapping("/api/files")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final int MIN_IMAGE_SIZE = 100; // 最小有效图片大小（字节）
    
    @Autowired
    private FileService fileService;
    
    @GetMapping("/{fileId}")
    public ResponseEntity<File> getFileInfo(@PathVariable Long fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(file);
    }

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long fileId) {
        try {
            logger.info("收到文件预览请求: fileId={}", fileId);
            
            // 获取文件信息
            File file = fileService.getFileById(fileId);
            if (file == null) {
                logger.error("文件不存在: fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件内容
            byte[] fileContent = fileService.previewFile(fileId);
            if (fileContent == null || fileContent.length == 0) {
                logger.error("文件内容为空: fileId={}", fileId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            
            logger.info("成功获取文件内容，大小: {} bytes", fileContent.length);
            
            // 构建响应头
            HttpHeaders headers = new HttpHeaders();
            
            // 根据文件类型设置适当的Content-Type
            String mediaTypeString = determineMediaType(file.getFileType(), file.getFileName());
            
            // 检查并确保mediaTypeString包含"/"
            if (mediaTypeString != null && !mediaTypeString.contains("/")) {
                logger.warn("MIME类型格式不正确，尝试根据文件名修正: {}", mediaTypeString);
                if (file.getFileName() != null) {
                    String extension = "";
                    int dotIndex = file.getFileName().lastIndexOf('.');
                    if (dotIndex > 0) {
                        extension = file.getFileName().substring(dotIndex + 1).toLowerCase();
                    }
                    
                    switch (extension) {
                        case "pdf":
                            mediaTypeString = "application/pdf";
                            break;
                        case "txt":
                            mediaTypeString = "text/plain";
                            break;
                        case "jpg":
                        case "jpeg":
                            mediaTypeString = "image/jpeg";
                            break;
                        case "png":
                            mediaTypeString = "image/png";
                            break;
                        // 确保所有可能的情况都被处理
                        default:
                            mediaTypeString = "application/octet-stream";
                            break;
                    }
                    logger.info("基于文件扩展名修正MIME类型: {}", mediaTypeString);
                } else {
                    // 如果没有文件名，使用通用二进制类型
                    mediaTypeString = "application/octet-stream";
                    logger.info("无法确定文件类型，使用默认类型: {}", mediaTypeString);
                }
            }
            
            // 设置内容类型
            headers.setContentType(MediaType.parseMediaType(mediaTypeString));
            
            // 对于文本文件，显式设置字符集为UTF-8以避免乱码
            if (isTextFile(file.getFileType(), file.getFileName()) || 
                (mediaTypeString != null && mediaTypeString.startsWith("text/"))) {
                headers.set(HttpHeaders.CONTENT_TYPE, mediaTypeString + ";charset=UTF-8");
                logger.info("检测到文本文件，设置字符集为UTF-8");
            }
            
            // 设置内容长度
            headers.setContentLength(fileContent.length);
            
            // 根据文件名设置Content-Disposition
            if (file.getFileName() != null) {
                String encodedFileName = java.net.URLEncoder.encode(file.getFileName(), "UTF-8")
                                           .replace("+", "%20");
                headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename*=UTF-8''" + encodedFileName);
            }
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (org.springframework.http.InvalidMediaTypeException e) {
            // 专门捕获MIME类型错误
            logger.error("MIME类型无效: {}", e.getMessage());
            
            // 创建一个通用响应，使用标准二进制类型
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 获取文件内容（再次尝试）
            byte[] fileContent = fileService.previewFile(fileId);
            if (fileContent != null && fileContent.length > 0) {
                return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            logger.error("预览文件出错: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String fileType, String fileName) {
        if (fileType != null) {
            String lowerType = fileType.toLowerCase();
            if (lowerType.contains("text") || lowerType.equals("application/json") || 
                lowerType.equals("application/xml")) {
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
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") || 
               lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") || 
               lowerFileName.endsWith(".bmp") || lowerFileName.endsWith(".webp") ||
               lowerFileName.endsWith(".svg");
    }
    
    /**
     * 根据文件类型和文件名确定适当的MediaType
     */
    private String determineMediaType(String fileType, String fileName) {
        // 如果已知文件MIME类型，且格式正确（包含"/"），直接使用
        if (fileType != null && !fileType.isEmpty() && !fileType.equalsIgnoreCase("null") && fileType.contains("/")) {
            return fileType;
        }
        
        // 如果文件类型不为空但格式不正确（不包含"/"），尝试修正
        if (fileType != null && !fileType.isEmpty() && !fileType.equalsIgnoreCase("null") && !fileType.contains("/")) {
            logger.info("检测到不规范的文件类型格式: {}, 尝试修正", fileType);
            String normalizedType = normalizeFileType(fileType);
            if (normalizedType != null) {
                logger.info("修正文件类型: {} -> {}", fileType, normalizedType);
                return normalizedType;
            }
        }
        
        // 根据文件名后缀推断MIME类型
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            
            if (lowerName.endsWith(".txt")) {
                return "text/plain";
            } else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
                return "text/html";
            } else if (lowerName.endsWith(".css")) {
                return "text/css";
            } else if (lowerName.endsWith(".js")) {
                return "application/javascript";
            } else if (lowerName.endsWith(".json")) {
                return "application/json";
            } else if (lowerName.endsWith(".xml")) {
                return "application/xml";
            } else if (lowerName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
                return "application/msword";
            } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
                return "application/vnd.ms-excel";
            } else if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
                return "application/vnd.ms-powerpoint";
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (lowerName.endsWith(".png")) {
                return "image/png";
            } else if (lowerName.endsWith(".gif")) {
                return "image/gif";
            } else if (lowerName.endsWith(".bmp")) {
                return "image/bmp";
            } else if (lowerName.endsWith(".svg")) {
                return "image/svg+xml";
            } else if (lowerName.endsWith(".mp3")) {
                return "audio/mpeg";
            } else if (lowerName.endsWith(".mp4")) {
                return "video/mp4";
            } else if (lowerName.endsWith(".avi")) {
                return "video/x-msvideo";
            } else if (lowerName.endsWith(".log") || lowerName.endsWith(".md")) {
                return "text/plain";
            }
        }
        
        // 默认使用二进制流类型
        return "application/octet-stream";
    }
    
    /**
     * 标准化文件类型格式，将简单类型转换为标准MIME类型
     */
    private String normalizeFileType(String fileType) {
        if (fileType == null) {
            return null;
        }
        
        String lowerType = fileType.toLowerCase().trim();
        
        switch (lowerType) {
            // 文档类型
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "txt":
                return "text/plain";
                
            // 图片类型    
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
                
            // 文本类型    
            case "html":
            case "htm":
                return "text/html";
            case "xml":
                return "application/xml";
            case "json":
                return "application/json";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
                
            // 音频类型    
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
                
            // 视频类型    
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
                
            default:
                // 如果无法识别，返回null让后续逻辑处理
                return null;
        }
    }
}