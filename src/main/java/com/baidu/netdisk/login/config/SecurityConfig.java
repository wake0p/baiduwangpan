package com.baidu.netdisk.login.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/login", "/generateCaptcha").permitAll() // 放行自定义接口
                .anyRequest().permitAll()
                .and()
                .formLogin().disable() // <-- 禁用默认表单登录
                .logout().disable()    // 禁用默认登出
                .csrf().disable();    // <-- 禁用 CSRF（开发环境）

        return http.build();
    }
}
