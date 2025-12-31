package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eisoo.standardization.common.enums.CatalogTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.entity
 * @Date: 2023/5/13 14:45
 */
@Data
@TableName("t_task")
@ApiModel
public class TaskEntity implements Serializable {
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
    @ApiModelProperty(value="任务对象id",dataType = "java.lang.String")
    @TableField(value ="f_object_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String objectId;

    /**
     * 任务对象类型
     */
    @ApiModelProperty(value="任务对象类型",dataType = "java.lang.Integer")
    @TableField(value ="f_object_type")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer objectType;

    /**
     * 表名称
     */
    @ApiModelProperty(value = "字段名称", example = "name", dataType = "java.lang.String")
    @TableField(value = "f_name")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String name;

    /**
     *任务状态
     */
    @ApiModelProperty(value="任务状态",notes="：0成功、1失败、2进行中",dataType = "java.lang.Integer")
    @TableField(value ="f_status")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer status;

    /**
     * 任务开始时间
     */
    @TableField(value = "f_start_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "任务开始时间", example = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 任务结束时间
     */
    @TableField(value = "f_end_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "任务结束时间", example = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 创建用户
     */
    @ApiModelProperty(value="创建用户",dataType = "java.lang.String")
    @TableField(value ="f_create_user")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createUser;

    /**
     * 权限域（目前为预留字段）
     */
    @ApiModelProperty(value="权限域（目前为预留字段）")
    @TableField(value ="f_authority_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long authorityId;

    /**
     * 高级参数，默认为"[]"，格式为"[{key:key1, value:value1}]"
     */
    @ApiModelProperty(value="高级参数")
    @TableField(value = "f_advanced_params")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String advancedParams;

}
