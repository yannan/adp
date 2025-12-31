package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.entity
 * @Date: 2023/7/7 14:18
 */
@Data
@TableName("t_task_log")
@ApiModel
public class TaskLogEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 唯一id，雪花算法
     */
    @ApiModelProperty(value="唯一标识",dataType = "java.lang.String")
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /**
     * 任务对象id
     */
    @ApiModelProperty(value="任务id",dataType = "java.lang.String")
    @TableField(value ="f_task_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long taskId;

    /**
     * 任务日志
     */
    @ApiModelProperty(value = "字段名称", example = "name", dataType = "java.lang.String")
    @TableField(value = "f_log")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String log;

    /**
     * 权限域（目前为预留字段）
     */
    @ApiModelProperty(value="权限域（目前为预留字段）")
    @TableField(value ="f_authority_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long authorityId;
}
