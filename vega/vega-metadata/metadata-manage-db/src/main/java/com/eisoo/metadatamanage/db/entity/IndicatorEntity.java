package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.vo
 * @Date: 2023/5/10 9:49
 */
@Data
@ApiModel
@TableName("t_indicator")
public class IndicatorEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识、雪花算法
     */
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value="唯一标识",dataType = "java.lang.String")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 指标名称
     */
    @ApiModelProperty(value="指标名称",dataType = "java.lang.String")
    @TableField(value ="f_indicator_name")
    private String indicatorName;

    /**
     * 指标类型
     */
    @ApiModelProperty(value = "指标类型", example = "", dataType = "java.lang.String")
    @TableField(value ="f_indicator_type")
    private String indicatorType;

    /**
     * 指标数值
     */
    @ApiModelProperty(value = "指标数值", example = "1111", dataType = "java.lang.String")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableField(value ="f_indicator_value")
    private Long indicatorValue;

    /**
     * 创建时间
     */
    @TableField(value = "f_create_time")
    @ApiModelProperty(value="创建时间",dataType = "java.lang.String")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 关联对象ID
     */
    @ApiModelProperty(value = "关联对象ID", example = "1111111", dataType = "java.lang.String")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableField(value ="f_indicator_object_id")
    private Long indicatorObjectId;

    /**
     * 权限域（目前为预留字段），默认0
     */
    @TableField(value = "f_authority_id")
    private Long authorityId;

    /**
     * 高级参数，默认为"[]"，格式为"[{key:key1, value:value1}]"
     */
    @TableField(value = "f_advanced_params")
    private String advancedParams;

}
