package com.baidu.netdisk.entity;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fileId;            // 分享的文件ID
    private Long userId;            // 分享用户ID
    private Long receiverId;        // 接收用户ID（针对私密分享）
    private String shareCode;       // 分享码
    private String shareType;       // 分享类型：public-公开，private-私密
    private Integer viewCount;      // 访问次数
    private Integer downloadCount;  // 下载次数
    private String fileName;        // 文件名（冗余字段，便于查询）
    private LocalDateTime expireTime; // 过期时间
    private Integer status;         // 状态：0-失效，1-有效
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 