package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

import java.util.Date;

@Data
public class TaskDto {
    private static final long serialVersionUID = 1L;

    String taskName;

    String taskId;
    /**
     * 唯一id，雪花算法
     */
    private Long id;

    /**
     * 任务对象id
     */
    private String objectId;

    /**
     * 任务对象类型
     */
    private Integer objectType;

    /**
     * 表名称
     */
    private String name;

    /**
     *任务状态
     */
    private Integer status;

    /**
     * 任务开始时间
     */
    private Date startTime;

    /**
     * 任务结束时间
     */

    private Date endTime;

    /**
     * 创建用户
     */
    private String createUser;

    /**
     * 权限域（目前为预留字段）
     */
    private Long authorityId;

    /**
     * 高级参数，默认为"[]"，格式为"[{key:key1, value:value1}]"
     */
    private String advancedParams;
}
