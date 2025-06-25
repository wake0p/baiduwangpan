package com.baidu.netdisk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fileId;
    private Long userId;
    private Long receiverId;        // 接收用户ID（针对私密分享）
    private String shareCode;
    private String shareType; // public/private
    private Integer viewCount;      // 访问次数
    private Integer downloadCount;  // 下载次数
    private String fileName;        // 文件名（冗余字段，便于查询）
    private LocalDateTime expireTime;
    private Integer status; // 1-有效 0-失效
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}