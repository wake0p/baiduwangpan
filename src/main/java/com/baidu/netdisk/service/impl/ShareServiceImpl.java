package com.baidu.netdisk.service.impl;

import com.baidu.netdisk.entity.NetdiskFile;
import com.baidu.netdisk.entity.Share;
import com.baidu.netdisk.repository.FileRepository;
import com.baidu.netdisk.repository.ShareRepository;
import com.baidu.netdisk.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShareServiceImpl implements ShareService {

    @Autowired
    private ShareRepository shareRepository;
    
    @Autowired
    private FileRepository fileRepository;

    @Override
    @Transactional
    public Share createShare(Long userId, Long fileId, String shareType, Long receiverId, Integer expireDays) {
        // 获取文件信息
        NetdiskFile file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return null;
        }
        
        // 创建分享记录
        Share share = new Share();
        share.setFileId(fileId);
        share.setUserId(userId);
        share.setReceiverId(receiverId);
        share.setShareType(shareType);
        share.setShareCode(generateShareCode());
        share.setFileName(file.getFileName());
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setStatus(1); // 1表示有效
        
        // 设置过期时间
        LocalDateTime now = LocalDateTime.now();
        share.setCreateTime(now);
        share.setUpdateTime(now);
        
        if (expireDays != null && expireDays > 0) {
            share.setExpireTime(now.plusDays(expireDays));
        } else {
            // 默认7天过期
            share.setExpireTime(now.plusDays(7));
        }
        
        return shareRepository.save(share);
    }

    @Override
    public List<Share> getMyShares(Long userId, Pageable pageable) {
        return shareRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }

    @Override
    public List<Share> getReceivedShares(Long userId, Pageable pageable) {
        return shareRepository.findReceivedSharesByUserId(userId, pageable);
    }

    @Override
    public List<Share> searchMyShares(Long userId, String query, Pageable pageable) {
        return shareRepository.searchMyShares(userId, query, pageable);
    }

    @Override
    public List<Share> searchReceivedShares(Long userId, String query, Pageable pageable) {
        return shareRepository.searchReceivedShares(userId, query, pageable);
    }

    @Override
    @Transactional
    public boolean deleteShare(Long shareId) {
        try {
            shareRepository.deleteById(shareId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Share getShareById(Long shareId) {
        return shareRepository.findById(shareId).orElse(null);
    }

    @Override
    public Share getShareByCode(String shareCode) {
        return shareRepository.findByShareCode(shareCode);
    }

    @Override
    @Transactional
    public void recordShareView(Long shareId) {
        Share share = shareRepository.findById(shareId).orElse(null);
        if (share != null) {
            share.setViewCount(share.getViewCount() + 1);
            shareRepository.save(share);
        }
    }

    @Override
    @Transactional
    public void recordShareDownload(Long shareId) {
        Share share = shareRepository.findById(shareId).orElse(null);
        if (share != null) {
            share.setDownloadCount(share.getDownloadCount() + 1);
            shareRepository.save(share);
        }
    }

    @Override
    public Map<String, Object> getShareDetail(Long shareId) {
        Share share = shareRepository.findById(shareId).orElse(null);
        if (share == null) {
            return null;
        }
        
        NetdiskFile file = fileRepository.findById(share.getFileId()).orElse(null);
        if (file == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("share", share);
        result.put("file", file);
        
        return result;
    }
    
    /**
     * 生成唯一的分享码
     */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
} 