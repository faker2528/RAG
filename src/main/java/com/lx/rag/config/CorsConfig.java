package com.lx.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("加载跨域配置...");
        registry.addMapping("/**") // 对所有路径生效
                .allowCredentials(true) // 允许携带凭证（如Cookie、Token）
                .allowedOrigins("http://localhost:5173/") // 仅允许指定前端域名（生产环境替换为实际域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 包含预检请求方法OPTIONS
                .allowedHeaders("*") // 允许所有请求头（也可指定具体头，如Content-Type, Authorization）
                .exposedHeaders("Content-Disposition") // 明确需要暴露的响应头（按需添加）
                .maxAge(3600); // 预检请求缓存时间（1小时，减少重复预检请求）
    }
}