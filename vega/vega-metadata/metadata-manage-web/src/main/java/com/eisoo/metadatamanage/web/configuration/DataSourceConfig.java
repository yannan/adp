package com.eisoo.metadatamanage.web.configuration;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.eisoo.metadatamanage.web.commons.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.druid")
public class DataSourceConfig {
    @Value("${spring.db-config.type}")
    private String type;
    @Value("${spring.db-config.url}")
    private String url;
    @Value("${spring.db-config.username}")
    private String username;
    @Value("${spring.db-config.password}")
    private String password;
    @Resource
    private DruidPoolConfig druidPoolConfig;

    @Bean
    public DataSource dataSource() throws Exception {
        String driverClassName = Constants.MARIADB_DRIVER;
        if (type.equals("mariadb")) {
            driverClassName = Constants.MARIADB_DRIVER;
        } else if ("dm8".equalsIgnoreCase(type)) {
            driverClassName = Constants.DEMENG_DRIVER;
        }
        Properties props = new Properties();
        props.put("url", url);
        log.info("type:{};url:{}", type, url);
        props.put("username", username);
        props.put("password", password);
        props.put("driverClassName", driverClassName);
        DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);
        DruidDataSourceFactory.config(dataSource, druidPoolConfig.getPool());
        return DruidDataSourceFactory.createDataSource(props);
    }
}
