package com.baidu.netdisk.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "folder")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String folderName;     // 文件夹名称
    private Long parentId;         // 父文件夹ID，0表示根目录
    private Long userId;           // 所属用户ID
    private Integer status;        // 状态：0-删除，1-正常
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
} 