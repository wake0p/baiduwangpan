package com.baidu.netdisk.fileop.service;

import com.baidu.netdisk.entity.Share;
import com.baidu.netdisk.fileop.Mapper.ShareMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class ShareService {

    @Autowired
    private ShareMapper shareMapper;

    // 生成分享（支持加密、公开，设置过期时间）
    @Transactional
    public Share createShare(Long fileId, Long userId, String shareType, LocalDateTime expireTime) {
        Share share = new Share();
        share.setFileId(fileId);
        share.setUserId(userId);
        share.setShareType(shareType);
        share.setExpireTime(expireTime);
        share.setStatus(1); // 1-有效
        share.setCreateTime(LocalDateTime.now());
        share.setUpdateTime(LocalDateTime.now());

        // 加密分享时生成4位密码（字母+数字）
        if ("private".equals(shareType)) {
            share.setShareCode(RandomStringUtils.randomAlphanumeric(4));
        } else {
            share.setShareCode(""); // 公开分享无密码
        }

        shareMapper.insertShare(share);
        return share;
    }

    // 查询文件的分享记录
    public List<Share> getSharesByFileId(Long fileId) {
        return shareMapper.getSharesByFileId(fileId);
    }

    // 删除分享
    @Transactional
    public void deleteShare(Long shareId) {
        shareMapper.deleteShare(shareId);
    }

    // 校验分享是否有效（过期、状态）
    public boolean checkShareValid(Long shareId) {
        Share share = shareMapper.getShareById(shareId);
        if (share == null || share.getStatus() == 0) {
            return false;
        }
        // 判断是否过期
        return share.getExpireTime().isAfter(LocalDateTime.now());
    }
    public boolean verifyPassword(Long shareId, String password) {
        Share share = shareMapper.getShareById(shareId);
        if (share != null && "private".equals(share.getShareType())) {
            return password.equals(share.getShareCode());
        }
        return "public".equals(share.getShareType());
    }
}