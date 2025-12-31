package com.eisoo.metadatamanage.web.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.db-config")
public class DruidPoolConfig {
    private Map<String, Object> pool = new HashMap<>();
}
