package com.baidu.netdisk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Folder {
    private Long id;
    private String folderName;      // 文件夹名称
    private Long parentId;          // 父文件夹ID
    private Long userId;            // 创建用户ID
    private Integer status;         // 状态：0-删除，1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 