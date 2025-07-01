package com.baidu.netdisk.login.controller;

import ch.qos.logback.core.net.server.Client;
import com.baidu.netdisk.entity.User;
import com.baidu.netdisk.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{8,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");

    private static final String UPLOAD_DIR = "uploads/avatars";

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 从Session获取存储的验证码
        String storedCaptcha = (String) request.getSession().getAttribute("captcha");
        String captchaInput = loginData.get("captcha");

        // 验证验证码
        if (storedCaptcha == null || !storedCaptcha.equals(captchaInput)) {
            result.put("success", false);
            result.put("errorType", "captcha");
            result.put("message", "验证码错误" + storedCaptcha + " " + captchaInput);
            return result;
        }

        // 2. 验证用户名和密码
        String username = loginData.get("username");
        String password = loginData.get("password");

        User user = userService.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        // 新增：验证用户是否被封禁
        if (user.getStatus() == 0) {
            result.put("success", false);
            result.put("message", "该用户已被封禁，无法登录");
            return result;
        }

        // 3. 登录成功，生成Token或设置Session
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("user", user);
        result.put("isAdmin", user.getIsAdmin());

        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> validationResult = new HashMap<>();

        // 用户名校验
        if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            validationResult.put("username", "×");
        } else if (userService.isUsernameExists(user.getUsername())) {
            validationResult.put("username", "×");
            result.put("success", false);
            result.put("message", "输入账号已存在，请重新输入");
            result.put("validation", validationResult);
            return result;
        } else {
            validationResult.put("username", "√");
        }

        // 密码校验
        if (!PASSWORD_PATTERN.matcher(user.getPassword()).matches()) {
            validationResult.put("password", "×");
        } else {
            validationResult.put("password", "√");
        }

        // 手机号校验
        if (!PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            validationResult.put("phone", "×");
        } else {
            validationResult.put("phone", "√");
        }

        // 昵称校验
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            validationResult.put("nickname", "×");
        } else {
            validationResult.put("nickname", "√");
        }

        // 检查是否有校验失败的项
        if (validationResult.containsValue("×")) {
            result.put("success", false);
            result.put("message", "输入信息存在错误，请检查");
            result.put("validation", validationResult);
            return result;
        }

        if (userService.register(user)) {
            result.put("success", true);
            result.put("message", "注册成功，请登录！");
        } else {
            result.put("success", false);
            result.put("message", "注册失败，请稍后重试");
        }
        return result;
    }

    @GetMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        result.put("success", true);
        result.put("message", "退出登录成功");
        return result;
    }

    @PostMapping("/verifyUserInfo")
    public Map<String, Object> verifyUserInfo(@RequestBody Map<String, String> userInfo) {
        Map<String, Object> result = new HashMap<>();
        String username = userInfo.get("username");
        String phone = userInfo.get("phone");

        User user = userService.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "未找到该用户名对应的用户");
        } else if (!user.getPhone().equals(phone)) {
            result.put("success", false);
            result.put("message", "用户名和手机号不匹配");
        } else {
            result.put("success", true);
            result.put("message", "验证成功");
        }
        return result;
    }

    @PostMapping("/findPassword")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> changePasswordData) {
        Map<String, Object> result = new HashMap<>();
        String username = changePasswordData.get("username");
        String newPassword = changePasswordData.get("newPassword");

        User user = userService.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "未找到该用户名对应的用户");
        } else {
            user.setPassword(newPassword);
            user.setUpdateTime(java.time.LocalDateTime.now());
            userService.save(user);
            result.put("success", true);
            result.put("message", "密码修改成功");
        }
        return result;
    }
    // 获取个人信息
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestParam String username) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.findByUsername(username);
        if (user != null) {
            result.put("success", true);
            result.put("user", user);
        } else {
            result.put("success", false);
            result.put("message", "未找到该用户");
        }
        return result;
    }

    // 修改密码
    @PostMapping("/changePassword")
    public Map<String, Object> changePassword(@RequestParam String username, @RequestParam String oldPassword, @RequestParam String newPassword) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.updatePassword(username, oldPassword, newPassword);
        if (user != null) {
            result.put("success", true);
            result.put("message", "密码修改成功");
        } else {
            result.put("success", false);
            result.put("message", "旧密码错误或用户不存在");
        }
        return result;
    }

    // 修改电话
    @PostMapping("/changePhone")
    public Map<String, Object> changePhone(@RequestParam String username, @RequestParam String newPhone) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.updatePhone(username, newPhone);
        if (user != null) {
            result.put("success", true);
            result.put("message", "电话修改成功");
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        return result;
    }

    // 修改头像
    @PostMapping("/changeAvatar")
    public Map<String, Object> changeAvatar(@RequestParam String username, @RequestParam("newAvatar") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的头像文件");
            return result;
        }

        try {
            // 创建存储目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一的文件名
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件
            file.transferTo(filePath);

            // 获取文件的存储路径
            String avatarUrl = UPLOAD_DIR + "/" + fileName;

            // 更新用户头像信息
            User user = userService.updateAvatar(username, avatarUrl);

            if (user != null) {
                result.put("success", true);
                result.put("message", "头像修改成功");
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件上传失败: " + e.getMessage());
        }

        return result;
    }
    @PostMapping("/banUser")
    public Map<String, Object> banUser(@RequestBody Map<String, String> banData) {
        Map<String, Object> result = new HashMap<>();
        String username = banData.get("username");
        User user = userService.banUser(username);
        if (user != null) {
            result.put("success", true);
            result.put("message", "用户封禁成功");
        } else {
            result.put("success", false);
            result.put("message", "未找到该用户");
        }
        return result;
    }
    @GetMapping("/userList")
    public Map<String, Object> getUserList() {
        Map<String, Object> result = new HashMap<>();
        List<User> userList = userService.getAllUsers();
        result.put("success", true);
        result.put("userList", userList);
        return result;
    }

}