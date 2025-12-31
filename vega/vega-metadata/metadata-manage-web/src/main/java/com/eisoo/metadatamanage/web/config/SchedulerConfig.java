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
public class SchedulerConfig {
    @Value("${scheduler.trigger:false}")
    private Boolean trigger;
    @Value("${scheduler.cron}")
    private String cron;
    @Value("${scheduler.projectName}")
    private String projectName;
    @Value("${scheduler.taskName}")
    private String taskName;
    @Value("${scheduler.taskUuid}")
    private String taskUuid;
    @Value("${scheduler.processName}")
    private String processName;
    @Value("${scheduler.processUuid}")
    private String processUuid;
    @Value("${scheduler.taskUrl}")
    private String taskUrl;
    @Value("${scheduler.token}")
    private String token;
    @Value("${scheduler.schedulerModelUrl}")
    private String schedulerModelUrl;
    @Value("${scheduler.schedulerProcessUrl}")
    private String schedulerProcessUrl;

    @Value("${scheduler.autoMonitorTrigger:false}")
    private Boolean autoMonitorTrigger;
}
