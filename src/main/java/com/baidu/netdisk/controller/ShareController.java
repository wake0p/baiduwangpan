package com.baidu.netdisk.controller;

import com.baidu.netdisk.entity.Share;
import com.baidu.netdisk.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shares")
public class ShareController {

    @Autowired
    private ShareService shareService;

    /**
     * 创建分享链接
     */
    @PostMapping("/create")
    public ResponseEntity<Share> createShare(@RequestBody Map<String, Object> shareData) {
        Long userId = Long.parseLong(shareData.get("userId").toString());
        Long fileId = Long.parseLong(shareData.get("fileId").toString());
        String shareType = shareData.get("shareType").toString();
        Long receiverId = shareData.get("receiverId") != null ? Long.parseLong(shareData.get("receiverId").toString()) : null;
        Integer expireDays = shareData.get("expireDays") != null ? Integer.parseInt(shareData.get("expireDays").toString()) : 7;

        Share share = shareService.createShare(userId, fileId, shareType, receiverId, expireDays);
        if (share != null) {
            return ResponseEntity.ok(share);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取用户创建的分享
     */
    @GetMapping("/my/{userId}")
    public ResponseEntity<List<Share>> getMyShares(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Share> shares = shareService.getMyShares(userId, pageable);
        return ResponseEntity.ok(shares);
    }

    /**
     * 获取用户接收到的分享
     */
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<Share>> getReceivedShares(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Share> shares = shareService.getReceivedShares(userId, pageable);
        return ResponseEntity.ok(shares);
    }

    /**
     * 搜索我的分享
     */
    @GetMapping("/my/{userId}/search")
    public ResponseEntity<List<Share>> searchMyShares(
            @PathVariable Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Share> shares = shareService.searchMyShares(userId, query, pageable);
        return ResponseEntity.ok(shares);
    }

    /**
     * 搜索接收到的分享
     */
    @GetMapping("/received/{userId}/search")
    public ResponseEntity<List<Share>> searchReceivedShares(
            @PathVariable Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Share> shares = shareService.searchReceivedShares(userId, query, pageable);
        return ResponseEntity.ok(shares);
    }

    /**
     * 删除分享
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Map<String, String>> deleteShare(@PathVariable Long shareId) {
        boolean result = shareService.deleteShare(shareId);
        Map<String, String> response = new HashMap<>();
        
        if (result) {
            response.put("message", "分享删除成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "分享删除失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量删除分享
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteShare(@RequestBody List<Long> shareIds) {
        int success = 0;
        for (Long shareId : shareIds) {
            if (shareService.deleteShare(shareId)) {
                success++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalCount", shareIds.size());
        response.put("successCount", success);
        response.put("message", String.format("成功删除 %d/%d 个分享", success, shareIds.size()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 通过ID获取分享
     */
    @GetMapping("/{shareId}")
    public ResponseEntity<Share> getShareById(@PathVariable Long shareId) {
        Share share = shareService.getShareById(shareId);
        if (share != null) {
            return ResponseEntity.ok(share);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 通过分享码获取分享
     */
    @GetMapping("/code/{shareCode}")
    public ResponseEntity<Share> getShareByCode(@PathVariable String shareCode) {
        Share share = shareService.getShareByCode(shareCode);
        if (share != null) {
            // 记录访问次数
            shareService.recordShareView(share.getId());
            return ResponseEntity.ok(share);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取分享的详细信息
     */
    @GetMapping("/{shareId}/detail")
    public ResponseEntity<Map<String, Object>> getShareDetail(@PathVariable Long shareId) {
        Map<String, Object> detail = shareService.getShareDetail(shareId);
        if (detail != null) {
            return ResponseEntity.ok(detail);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 记录分享下载
     */
    @PostMapping("/{shareId}/download")
    public ResponseEntity<Void> recordShareDownload(@PathVariable Long shareId) {
        shareService.recordShareDownload(shareId);
        return ResponseEntity.ok().build();
    }
} 