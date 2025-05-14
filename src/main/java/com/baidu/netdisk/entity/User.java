package com.baidu.netdisk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private Long totalSpace;    // 总存储空间（字节）
    private Long usedSpace;     // 已使用空间（字节）
    private Integer status;     // 状态：0-禁用，1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
