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
public class CustomerDepETLColumn extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String type;
    private String host;
    private String user;
    private Integer port;
    @JsonProperty(value = "database_name")
    private String databaseName;
    @JsonProperty(value = "schema_name")
    private String schemaName;
    @JsonProperty(value = "table_name")
    private String tableName;
    @JsonProperty(value = "column_name")
    private String columnName;
}
