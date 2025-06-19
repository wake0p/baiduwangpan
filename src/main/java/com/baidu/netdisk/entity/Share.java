package com.baidu.netdisk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Share {
    private Long id;
    private Long fileId;
    private Long userId;
    private String shareCode;
    private String shareType; // public/private
    private LocalDateTime expireTime;
    private Integer status; // 1-有效 0-失效
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}