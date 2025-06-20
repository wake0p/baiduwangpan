package com.baidu.netdisk.login.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class CaptchaController {

    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CAPTCHA_LENGTH = 4;

    @GetMapping("/generateCaptcha")
    public Map<String, String> generateCaptcha(HttpServletRequest request) {
        HttpSession session = request.getSession();

        // 生成随机验证码
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            captcha.append(CHAR_SET.charAt(random.nextInt(CHAR_SET.length())));
        }

        // 存储到Session
        session.setAttribute("captcha", captcha.toString());

        // 生成随机颜色用于显示
        String textColor = generateRandomColor();
        String bgColor = generateRandomLightColor();

        // 返回给前端显示的内容（可以添加干扰线等）
        Map<String, String> result = new HashMap<>();
        result.put("captchaDisplay", captcha.toString());
        result.put("textColor", textColor);
        result.put("bgColor", bgColor);
        return result;
    }

    // 生成随机颜色
    private String generateRandomColor() {
        Random random = new Random();
        return String.format("#%06X", (0xFFFFFF & random.nextInt()));
    }

    // 生成随机浅色
    private String generateRandomLightColor() {
        Random random = new Random();
        int r = 180 + random.nextInt(76); // 180-255
        int g = 180 + random.nextInt(76);
        int b = 180 + random.nextInt(76);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}