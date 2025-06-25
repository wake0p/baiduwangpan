package com.baidu.netdisk.preview.controller;

import com.baidu.netdisk.entity.User;
import com.baidu.netdisk.preview.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}/space-info")
    public ResponseEntity<User> getUserSpaceInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            // 为了安全，只返回空间信息，不返回敏感信息
            User spaceInfo = new User();
            spaceInfo.setTotalSpace(user.getTotalSpace());
            spaceInfo.setUsedSpace(user.getUsedSpace());
            return ResponseEntity.ok(spaceInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            // 返回所有个人信息，但不包括密码等敏感信息
            User userInfo = new User();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setEmail(user.getEmail());
            userInfo.setPhone(user.getPhone());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setEmployeeId(user.getEmployeeId());
            userInfo.setDepartment(user.getDepartment());
            userInfo.setPosition(user.getPosition());
            userInfo.setCompanyEmail(user.getCompanyEmail());
            userInfo.setIsAdmin(user.getIsAdmin());
            userInfo.setNickname(user.getNickname());
            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 修改用户密码
     */
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {
        
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "旧密码和新密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "密码修改成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "密码修改失败，请确认旧密码是否正确");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改用户头像
     */
    @PostMapping("/{userId}/change-avatar")
    public ResponseEntity<Map<String, String>> changeAvatar(
            @PathVariable Long userId,
            @RequestBody Map<String, String> avatarData) {
        
        String avatarId = avatarData.get("avatarId");
        
        if (avatarId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "头像ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 根据avatarId获取头像URL
        String avatarUrl = getAvatarUrlById(avatarId);
        boolean success = userService.changeAvatar(userId, avatarUrl);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "头像修改成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "头像修改失败");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改用户电话
     */
    @PostMapping("/{userId}/change-phone")
    public ResponseEntity<Map<String, String>> changePhone(
            @PathVariable Long userId,
            @RequestBody Map<String, String> phoneData) {
        
        String phone = phoneData.get("phone");
        
        if (phone == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "电话号码不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = userService.changePhone(userId, phone);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "电话号码修改成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "电话号码修改失败");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据头像ID获取头像URL
     */
    private String getAvatarUrlById(String avatarId) {
        // 这里应该根据实际情况返回头像URL
        // 简单示例：假设头像放在固定位置
        return "/images/avatars/" + avatarId + ".png";
    }
}