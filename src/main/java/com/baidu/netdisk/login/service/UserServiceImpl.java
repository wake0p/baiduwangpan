package com.baidu.netdisk.login.service;

import com.baidu.netdisk.entity.User;
import com.baidu.netdisk.login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean register(User user) {
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    @Override
    public void save(User user) {
        userRepository.save(user);
    }
    @Override
    public User updatePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(oldPassword)) {
            user.setPassword(newPassword);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
        }
        return user;
    }

    @Override
    public User updatePhone(String username, String newPhone) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setPhone(newPhone);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
        }
        return user;
    }

    @Override
    public User updateAvatar(String username, String newAvatar) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setAvatar(newAvatar);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    @Override
    public User banUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setStatus(0); // 设置用户状态为禁用
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
        }
        return user;
    }

}

