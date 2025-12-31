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
@TableName(value = "t_lineage_tag_column2")
public class ColumnLineageEntity extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId("unique_id")
    @JsonProperty(value = "unique_id")
    private String uniqueId;

    @JsonProperty(value = "uuid")
    @TableField(value = "uuid")
    private String uuid;

    @JsonProperty(value = "technical_name")
    @TableField("technical_name")
    private String technicalName;


    @JsonProperty(value = "business_name")
    @TableField("business_name")
    private String businessName;


    private String comment;

    @JsonProperty(value = "data_type")
    @TableField("data_type")
    private String dataType;

    @JsonProperty(value = "primary_key")
    @TableField("primary_key")
    private String primaryKey;


    @JsonProperty(value = "table_unique_id")
    @TableField("table_unique_id")
    private String tableUniqueId;

    @JsonProperty(value = "expression_name", defaultValue = "")
    @TableField(value = "expression_name")
    private String expressionName;


    @JsonProperty(value = "column_unique_ids", defaultValue = "")
    @TableField(value = "column_unique_ids")
    private String columnUniqueIds = "";

    @JsonProperty(value = "action_type")
    @TableField(value = "action_type")
    private String actionType="insert";

    @JsonProperty(value = "created_at")
    @TableField(value = "created_at")
    private String createdAt;
    @JsonProperty(value = "updated_at")
    @TableField(value = "updated_at")
    private String updatedAt;
}
