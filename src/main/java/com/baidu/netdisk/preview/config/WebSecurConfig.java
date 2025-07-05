package com.baidu.netdisk.preview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurConfig {

    @Bean(name = "previewSecurityFilterChain")  // 改了名字，避免冲突
    public SecurityFilterChain previewSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/**").permitAll()
                .anyRequest().permitAll()
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .headers().frameOptions().disable();

        return http.build();
    }
}
