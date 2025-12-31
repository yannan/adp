package com.eisoo.metadatamanage.web.config;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.eisoo.metadatamanage.web.configuration.AnyRobotConfigruation;
import com.eisoo.standardization.common.mybatis.interceptor.ARTraceSqlStatementInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis plus 分页插件配置
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {
    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //	mybatis-plus 的 分页查询
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MARIADB));

        return interceptor;
    }

    /**
     * Mybatis 链路追踪，打印SQL 逻辑
     */
    @Autowired
    AnyRobotConfigruation anyRobotConfigruation;

    @Bean
    ARTraceSqlStatementInterceptor sqlStatementInterceptor() {
        String traceEndpointUrl = anyRobotConfigruation.getTraceEnabled() == false ? null : anyRobotConfigruation.getTraceEndpointUrl();
        return new ARTraceSqlStatementInterceptor(traceEndpointUrl, anyRobotConfigruation.getServiceName(), anyRobotConfigruation.getServiceVersion(), "mariadb");
    }

    @Bean
    public GlobalConfig globalConfiguration() {
        GlobalConfig conf = new GlobalConfig();
        // 自定义的注入需要在这里进行配置
        conf.setSqlInjector(easySqlInjector());
        return conf;
    }

    @Bean
    public EasySqlInjector easySqlInjector() {
        return new EasySqlInjector();
    }
}
