package com.baidu.netdisk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Share {
    private Long id;
    private Long fileId;            // 分享的文件ID
    private Long userId;            // 分享用户ID
    private String shareCode;       // 分享码
    private String shareType;       // 分享类型：public-公开，private-私密
    private LocalDateTime expireTime; // 过期时间
    private Integer status;         // 状态：0-失效，1-有效
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 