package com.baidu.netdisk.login.service;

import com.baidu.netdisk.entity.User;

import java.util.List;


public interface UserService {
    boolean register(User user);
    boolean isUsernameExists(String username);
    User findByUsername(String username);
    void save(User user);
    User updatePassword(String username, String oldPassword, String newPassword);
    User updatePhone(String username, String newPhone);
    User updateAvatar(String username, String newAvatar);
    List<User> getAllUsers();
    User banUser(String username);
}

