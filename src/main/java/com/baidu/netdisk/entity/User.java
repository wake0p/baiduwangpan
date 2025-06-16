package com.baidu.netdisk.entity;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;
@Data
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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