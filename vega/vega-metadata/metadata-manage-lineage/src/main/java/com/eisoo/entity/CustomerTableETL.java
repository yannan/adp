package com.eisoo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 15:02
 * @Version:1.0
 */
@Data
public class CustomerTableETL extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String type;
    private String host;
    private Integer port;
    @JsonProperty(value = "database_name")
    private String databaseName;
    @JsonProperty(value = "schema_name")
    private String schemaName;
    @JsonProperty(value = "table_name")
    private String tableName;
    private String comment;
    @JsonProperty(value = "task_execution_info")
    private String taskExecutionInfo;
    @JsonProperty(value = "created_at")
    private String createdAt;
    @JsonProperty(value = "updated_at")
    private String updatedAt;
}
