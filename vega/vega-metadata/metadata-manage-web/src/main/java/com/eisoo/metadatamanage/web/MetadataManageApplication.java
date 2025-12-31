package com.eisoo.metadatamanage.web;

import com.eisoo.config.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableAsync
@EnableOpenApi
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})
@MapperScan({"com.eisoo.metadatamanage.db.mapper*", "com.eisoo.mapper*"})
@ComponentScan({"com.eisoo.**"})
@Import(SpringUtil.class)
@EnableTransactionManagement
@EnableConfigurationProperties
public class MetadataManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetadataManageApplication.class, args);
    }

}
