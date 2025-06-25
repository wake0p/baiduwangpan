package com.baidu.netdisk.preview.controller;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.entity.Share;
import com.baidu.netdisk.preview.service.FileService;
import com.baidu.netdisk.preview.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;
    
    @Autowired
    private ShareService shareService;

    @GetMapping("/category/{userId}/{fileType}")
    public List<File> getFilesByCategory(
            @PathVariable Long userId,
            @PathVariable String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        if ("all".equalsIgnoreCase(fileType)) {
            return fileService.getFilesByUserIdAndFileType(userId, null, pageable); // Assuming null or a special value for all types
        } else {
            return fileService.getFilesByUserIdAndFileType(userId, fileType, pageable);
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = fileService.downloadFile(fileId);
        if (data == null) {
            return ResponseEntity.internalServerError().build();
        }

        ByteArrayResource resource = new ByteArrayResource(data);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(data.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = fileService.downloadFile(fileId);
        if (data == null) {
            return ResponseEntity.internalServerError().build();
        }

        // Determine content type based on file type for preview
        MediaType contentType;
        if (file.getFileType().equalsIgnoreCase("image")) {
            contentType = MediaType.IMAGE_JPEG; // Or IMAGE_PNG, etc.
        } else if (file.getFileType().equalsIgnoreCase("video")) {
            contentType = MediaType.APPLICATION_OCTET_STREAM; // Placeholder, ideally video/mp4 etc.
        } else if (file.getFileType().equalsIgnoreCase("document")) {
            contentType = MediaType.APPLICATION_PDF; // Placeholder, ideally application/pdf, text/plain etc.
        } else if (file.getFileType().equalsIgnoreCase("audio")) {
            contentType = MediaType.parseMediaType("audio/mpeg"); // 假设是MP3格式
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(resource);
    }

    @PostMapping("/favorite/{fileId}")
    public ResponseEntity<Void> setFavoriteStatus(@PathVariable Long fileId, @RequestParam Boolean isFavorite) {
        fileService.setFileFavoriteStatus(fileId, isFavorite);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/favorites/{userId}")
    public List<File> getFavoriteFiles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fileService.getFavoriteFilesByUserId(userId, pageable);
    }

    @GetMapping("/search/{userId}")
    public List<File> searchFiles(
            @PathVariable Long userId,
            @RequestParam String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fileService.searchFilesByFileName(userId, fileName, pageable);
    }
    
    @PostMapping("/{fileId}/share")
    public ResponseEntity<Map<String, Object>> shareFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> shareData) {
        
        Long userId = Long.parseLong(shareData.get("userId").toString());
        String shareType = shareData.get("shareType").toString();
        Long receiverId = shareData.get("receiverId") != null ? 
                         Long.parseLong(shareData.get("receiverId").toString()) : null;
        Integer expireDays = shareData.get("expireDays") != null ? 
                            Integer.parseInt(shareData.get("expireDays").toString()) : 7;
        
        Share share = shareService.createShare(userId, fileId, shareType, receiverId, expireDays);
        
        Map<String, Object> response = new HashMap<>();
        if (share != null) {
            response.put("success", true);
            response.put("shareCode", share.getShareCode());
            response.put("message", "文件分享成功");
            response.put("shareUrl", "/share/" + share.getShareCode());
            response.put("share", share);
        } else {
            response.put("success", false);
            response.put("message", "文件分享失败");
        }
        
        return ResponseEntity.ok(response);
    }
}