package com.baidu.netdisk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class File {
    private Long id;
    private String fileName;        // 文件名
    private String fileType;        // 文件类型
    private Long fileSize;          // 文件大小（字节）
    private String filePath;        // 文件存储路径
    private String fileMd5;         // 文件MD5值
    private Long folderId;          // 所属文件夹ID
    private Long userId;            // 上传用户ID
    private Integer status;         // 状态：0-删除，1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 