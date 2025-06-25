package com.baidu.netdisk.preview.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 关闭CSRF保护
        http.csrf().disable()
            // 允许所有请求通过，不做认证
            .authorizeRequests()
            .anyRequest().permitAll()
            .and()
            // 禁用默认登录页
            .formLogin().disable()
            // 禁用HTTP Basic认证
            .httpBasic().disable();
    }
} 