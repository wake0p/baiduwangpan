package com.baidu.netdisk.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class File { // 文件信息
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;        // 文件名
    private String fileType;        // 文件类型
    private Long fileSize;          // 文件大小（字节）
    private String filePath;        // 文件存储路径
    private String fileMd5;         // 文件MD5值
    private Long folderId;          // 所属文件夹ID
    private Long userId;            // 上传用户ID
    private Boolean isFavorite;     // 是否收藏：true-收藏，false-未收藏
    
    @Column(nullable = false, updatable = false)
    private Boolean status;         // 状态：false-删除，true-正常
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    // ====== 回收站相关字段 ======
    private Boolean isDeleted;      // 是否删除到回收站：false-正常，true-已删除
    private LocalDateTime deletedTime; // 删除时间
    private Long deletedBy;         // 执行删除操作的用户ID
}