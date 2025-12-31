package com.eisoo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Lan Tian
 * @Date: 2025/1/7 15:39
 * @Version:1.0
 */
@Data
public class DolphinEntity extends BaseLineageEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    @JsonProperty(value = "source_table_id")
    private String sourceTableId;
    @JsonProperty(value = "target_table_id")
    private String targetTableId;
    @JsonProperty(value = "create_sql")
    private String createSql;
    @JsonProperty(value = "insert_sql")
    private String insertSql;
}
