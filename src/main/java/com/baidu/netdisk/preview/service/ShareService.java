package com.baidu.netdisk.preview.service;

import com.baidu.netdisk.preview.entity.Share;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ShareService {
    
    /**
     * 创建分享链接
     */
    Share createShare(Long userId, Long fileId, String shareType, Long receiverId, Integer expireDays);
    
    /**
     * 获取用户创建的分享
     */
    List<Share> getMyShares(Long userId, Pageable pageable);
    
    /**
     * 获取用户接收到的分享
     */
    List<Share> getReceivedShares(Long userId, Pageable pageable);
    
    /**
     * 搜索我的分享
     */
    List<Share> searchMyShares(Long userId, String query, Pageable pageable);
    
    /**
     * 搜索接收到的分享
     */
    List<Share> searchReceivedShares(Long userId, String query, Pageable pageable);
    
    /**
     * 删除分享
     */
    boolean deleteShare(Long shareId);
    
    /**
     * 通过ID获取分享
     */
    Share getShareById(Long shareId);
    
    /**
     * 通过分享码获取分享
     */
    Share getShareByCode(String shareCode);
    
    /**
     * 记录分享访问
     */
    void recordShareView(Long shareId);
    
    /**
     * 记录分享下载
     */
    void recordShareDownload(Long shareId);
    
    /**
     * 获取分享的详细信息（包括文件信息）
     */
    Map<String, Object> getShareDetail(Long shareId);
} 