package com.baidu.netdisk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("你的邮箱SMTP服务器地址"); // 比如 QQ邮箱是 smtp.qq.com
        mailSender.setPort(465); // 对应端口，不同邮箱可能不同
        mailSender.setUsername("你的发件人邮箱");
        mailSender.setPassword("你的邮箱授权码");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        return mailSender;
    }
}