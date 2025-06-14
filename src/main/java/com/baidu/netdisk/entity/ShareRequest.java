package com.baidu.netdisk.entity;

import java.time.LocalDateTime;

// 需根据实际需求调整包路径和属性
public class ShareRequest {
    private Long userId;
    private String shareType;
    private LocalDateTime expireTime;

    // 省略 get/set 方法
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getShareType() {
        return shareType;
    }
    public void setShareType(String shareType) {
        this.shareType = shareType;
    }
    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}