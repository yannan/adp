package com.eisoo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 15:02
 * @Version:1.0
 */
@Data
@TableName(value = "t_lineage_tag_table2")
@ToString
public class TableLineageEntity extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonProperty(value = "unique_id")
    @TableId("unique_id")
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

    @JsonProperty(value = "table_type")
    @TableField("table_type")
    private String tableType;
    @JsonProperty(value = "datasource_id")
    @TableField("datasource_id")
    private String datasourceId;

    @JsonProperty(value = "datasource_name")
    @TableField("datasource_name")
    private String datasourceName;

    @JsonProperty(value = "owner_id")
    @TableField("owner_id")
    private String ownerId;

    @JsonProperty(value = "owner_name")
    @TableField("owner_name")
    private String ownerName;

    @TableField("department_id")
    @JsonProperty(value = "department_id")
    private String departmentId;

    @JsonProperty(value = "department_name")
    @TableField("department_name")
    private String departmentName;

    @JsonProperty(value = "info_system_id")
    @TableField("info_system_id")
    private String infoSystemId;

    @JsonProperty(value = "info_system_name")
    @TableField("info_system_name")
    private String infoSystemName;

    @JsonProperty(value = "database_name")
    @TableField("database_name")
    private String databaseName;


    @JsonProperty(value = "catalog_name")
    @TableField("catalog_name")
    private String catalogName;

    @JsonProperty(value = "catalog_addr")
    @TableField("catalog_addr")
    private String catalogAddr;

    @JsonProperty(value = "catalog_type")
    @TableField("catalog_type")
    private String catalogType;

    @JsonProperty(value = "task_execution_info")
    @TableField(value = "task_execution_info")
    private String taskExecutionInfo;

    @JsonProperty(value = "action_type",defaultValue = "insert")
    @TableField("action_type")
    private String actionType = "insert";

    @JsonProperty(value = "created_at")
    @TableField(value ="created_at")
    private String createdAt;

    @JsonProperty(value = "updated_at")
    @TableField(value ="updated_at")
    private String updatedAt;

    public TableLineageEntity(String tableUniqueId, String tableType, String uuid, String catalogName, String schemaName, String tableName) {
        this.uniqueId = tableUniqueId;
        this.tableType = tableType;
        this.uuid = uuid;
        this.catalogName = catalogName;
        this.databaseName = schemaName;
        this.technicalName = tableName;
    }
}
