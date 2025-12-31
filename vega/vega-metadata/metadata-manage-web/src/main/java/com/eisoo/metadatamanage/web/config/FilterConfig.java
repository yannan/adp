package com.eisoo.metadatamanage.web.config;

import com.eisoo.metadatamanage.web.configuration.AnyRobotConfigruation;
import com.eisoo.standardization.common.webfilter.ArTraceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 作者: Jie.xu
 * 创建时间：2023/10/18 11:39
 * 功能描述：
 */
@Configuration
public class FilterConfig {

    @Autowired
    AnyRobotConfigruation anyRobotConfigruation;

    @Bean
    public FilterRegistrationBean<ArTraceFilter> arTraceFilter() {
        FilterRegistrationBean<ArTraceFilter> bean = new FilterRegistrationBean<>();
        String traceEndpointUrl = anyRobotConfigruation.getTraceEnabled() == false ? null : anyRobotConfigruation.getTraceEndpointUrl();
        bean.setFilter(new ArTraceFilter(traceEndpointUrl, anyRobotConfigruation.getServiceName(), anyRobotConfigruation.getServiceVersion()));
        bean.setName("ArTraceFilter");
        bean.setOrder(-1);
        return bean;

    }
}
