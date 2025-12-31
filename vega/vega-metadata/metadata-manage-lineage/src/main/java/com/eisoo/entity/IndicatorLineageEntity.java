package com.eisoo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 15:02
 * @Version:1.0
 */
@Data
@TableName(value = "t_lineage_tag_indicator2")
public class IndicatorLineageEntity extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId("uuid")
    private String uuid;

    @TableField(value = "name")
    private String name;

    @TableField(value = "description")
    private String description;

    @TableField(value = "code")
    private String code;

    @JsonProperty(value = "indicator_type")
    @TableField(value = "indicator_type")
    private String indicatorType;

    @TableField(value = "expression")
    private String expression;

    @JsonProperty(value = "indicator_uuids", defaultValue = "")
    @TableField(value = "indicator_uuids")
    private String indicatorUuids = "";

    @TableField(value = "time_restrict")
    @JsonProperty(value = "time_restrict")
    private String timeRestrict;

    @TableField(value = "modifier_restrict")
    @JsonProperty(value = "modifier_restrict")
    private String modifierRestrict;

    @TableField(value = "owner_uid")
    @JsonProperty(value = "owner_uid")
    private String ownerUid;

    @TableField(value = "owner_name")
    @JsonProperty(value = "owner_name")
    private String ownerName;

    @TableField(value = "department_id")
    @JsonProperty(value = "department_id")
    private String departmentId;

    @TableField(value = "department_name")
    @JsonProperty(value = "department_name")
    private String departmentName;

    @JsonProperty(value = "column_unique_ids", defaultValue = "")
    @TableField(value = "column_unique_ids")
    private String columnUniqueIds = "";

    @JsonProperty(value = "action_type")
    @TableField(value = "action_type")
    private String actionType = "insert";

    @JsonProperty(value = "created_at")
    @TableField(value = "created_at")
    private String createdAt;

    @JsonProperty(value = "updated_at")
    @TableField(value = "updated_at")
    private String updatedAt;
}
