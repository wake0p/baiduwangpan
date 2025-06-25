package com.baidu.netdisk.preview.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShareViewController {

    /**
     * 处理分享链接访问
     * 返回分享页面
     */
    @GetMapping("/share/{shareCode}")
    public String viewShare(@PathVariable String shareCode) {
        return "share.html";
    }
} 