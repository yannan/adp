package com.eisoo.metadatamanage.web.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.config
 * @Date: 2023/7/4 15:02
 */

@Data
@Configuration
public class DolphinschedulerConfig {

    @Value("${dolphinscheduler.host}")
    private String host;

    @Value("${dolphinscheduler.port}")
    private int port;

    @Value("${dolphinscheduler.token}")
    private String token;

    @Value("${dolphinscheduler.user}")
    private String user;

}
