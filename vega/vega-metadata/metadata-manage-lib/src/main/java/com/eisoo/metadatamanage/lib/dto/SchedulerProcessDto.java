package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

import java.util.List;

@Data
public class SchedulerProcessDto {
    private String process_uuid;

    /**
     * AF数据同步模型关系集合
     */
    private List<SchedulerProcessRelationDto> models;

    /**
     * 定时周期表达式
     */
    private String crontab;


    /**
     * 定时任务表达式生效状态，0失效，1生效
     */
    private Integer crontab_status;

    /**
     * 定时周期表达式生效时间
     */
    private String start_time;

    /**
     * 定时周期表达式失效时间
     */
    private String end_time;

    /**
     * 调度名称
     */
    private String process_name;

    /**
     * 0下线；1上线
     */
    private Integer online_status;

    /**
     *高级参数
     */
//    private String  advancedParams;
}
