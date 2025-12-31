package com.eisoo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
public class EngineConfiguration {
    @Value("${virtualization.host}")
    private String host;
    @Value("${virtualization.port}")
    private int port;
    @Value("${virtualization.columnApi}")
    private String columnApi;
    @Value("${virtualization.user}")
    private String user;
    @Value("${virtualization.protocol}")
    private String protocol;
}
