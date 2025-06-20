package com.baidu.netdisk.preview.service;

import com.baidu.netdisk.preview.entity.User;

public interface UserService {
    User getUserById(Long userId);
    User createUser(User user);
    void updateUserSpace(Long userId, long usedSpace);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    boolean changeAvatar(Long userId, String avatarUrl);
    boolean changePhone(Long userId, String phone);
}