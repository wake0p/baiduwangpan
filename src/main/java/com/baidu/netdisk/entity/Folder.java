package com.baidu.netdisk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Folder { // 文件夹信息
    private Long id;
    private String folderName;      // 文件夹名称
    private String folderPath;      // 文件夹路径
    private Long parentId;          // 父文件夹ID
    private Long userId;            // 创建用户ID
    private boolean status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")// 状态：0-删除，1-正常
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 最后更新时间

    // ====== 新增回收站相关字段 ======
    private boolean isDeleted;       // 是否删除到回收站：0-正常，1-已删除
    private LocalDateTime deletedTime; // 删除时间
    private Long deletedBy;          // 执行删除操作的用户ID


    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    // 对应的getIsDeleted方法
    public Boolean getIsDeleted() {
        return isDeleted;
    }
}