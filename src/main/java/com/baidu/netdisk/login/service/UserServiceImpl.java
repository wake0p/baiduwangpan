package com.baidu.netdisk.login.service;

import com.baidu.netdisk.entity.User;
import com.baidu.netdisk.login.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public boolean register(User user) {
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        usersRepository.save(user);
        return true;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return usersRepository.existsByUsername(username);
    }

    @Override
    public User findByUsername(String username) {
        return usersRepository.findByUsername(username);
    }
    @Override
    public void save(User user) {
        usersRepository.save(user);
    }
    @Override
    public User updatePassword(String username, String oldPassword, String newPassword) {
        User user = usersRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(oldPassword)) {
            user.setPassword(newPassword);
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }

    @Override
    public User updatePhone(String username, String newPhone) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            user.setPhone(newPhone);
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }

    @Override
    public User updateAvatar(String username, String newAvatar) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            user.setAvatar(newAvatar);
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return usersRepository.findAll();
    }
    @Override
    public User banUser(String username) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            user.setStatus(0); // 设置用户状态为禁用
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }
    @Override
    public void deleteUser(String username) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            usersRepository.delete(user);
        }
    }

    @Override
    public User unbanUser(String username) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            user.setStatus(1); // 设置用户状态为正常
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }
    @Override
    public User setAdmin(String username, boolean isAdmin) {
        User user = usersRepository.findByUsername(username);
        if (user != null) {
            user.setIsAdmin(isAdmin ? 1 : 0);
            user.setUpdateTime(LocalDateTime.now());
            usersRepository.save(user);
        }
        return user;
    }
}

