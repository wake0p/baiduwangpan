package com.baidu.netdisk.preview.service.impl;

import com.baidu.netdisk.entity.User;
import com.baidu.netdisk.preview.repository.UserRepository;
import com.baidu.netdisk.preview.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final long DEFAULT_TOTAL_SPACE = 10L * 1024 * 1024 * 1024; // 10GB 默认空间

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public User getUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.orElse(null);
    }

    @Override
    public User createUser(User user) {
        // 设置默认存储空间
        if (user.getTotalSpace() == null) {
            user.setTotalSpace(DEFAULT_TOTAL_SPACE);
        }
        if (user.getUsedSpace() == null) {
            user.setUsedSpace(0L);
        }
        // 对密码进行加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public void updateUserSpace(Long userId, long usedSpace) {
        User user = getUserById(userId);
        if (user != null) {
            user.setUsedSpace(usedSpace);
            userRepository.save(user);
        }
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
    
    @Override
    public boolean changeAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }
        
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return true;
    }
    
    @Override
    public boolean changePhone(Long userId, String phone) {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }
        
        user.setPhone(phone);
        userRepository.save(user);
        return true;
    }
}