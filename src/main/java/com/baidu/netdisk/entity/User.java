package com.baidu.netdisk.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    private String password;
    
    @Column(unique = true)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    private String avatar;
    private Long totalSpace;    // 总存储空间（字节）
    private Long usedSpace;     // 已使用空间（字节）
    private Integer status;     // 状态：0-禁用，1-正常
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    private String employeeId;  // 工号
    private String department;  // 部门
    private String position;    // 职位
    private String companyEmail; // 公司邮箱
    private Integer isAdmin = 0;    //管理员：1-是，0-不是，默认0
    private String nickname;       //昵称
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}