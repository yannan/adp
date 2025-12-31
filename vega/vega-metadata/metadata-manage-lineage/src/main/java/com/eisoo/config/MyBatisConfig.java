package com.eisoo.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/12 17:57
 * @Version:1.0
 */
@Configuration
public class MyBatisConfig {
    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> {
            // 注册自定义 TypeHandler
            configuration.getTypeHandlerRegistry().register(Set.class, com.eisoo.mapper.SetTypeHandler.class);
        };
    }
}
