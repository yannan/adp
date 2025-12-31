package com.eisoo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.SneakyThrows;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/11 10:31
 * @Version:1.0
 */

@Data
public class DolphinLineageEntity {
    private String code;
    private Integer total_count;
    private Map<String, List<DolphinSqlDomain>> data;
    @Data
    public static class DolphinSqlDomain implements Comparable<DolphinSqlDomain> {
        private String name;
        private String modelType;//Sync
        private String sourceCatalog;
        private String sourceSchema;
        private String sourceTableName;
        private List<DolphinSQLFieldDomain> sourceFields;
        private String targetCatalog;
        private String targetSchema;
        private String targetTableName;
        private List<DolphinSQLFieldDomain> targetFields;

        @JsonProperty("targetTableInsert")
        private String targetTableInsert;
        @JsonProperty("targetTableCreate")
        private String targetTableCreate;

        @JsonProperty("update_time")
        private String updateTime;//"2024-03-13 13:56:56


        @SneakyThrows
        @Override
        public int compareTo(final DolphinSqlDomain other) {
            if (null == other) {
                return 1;
            }
            return updateTime.compareTo(other.updateTime);
        }
    }

    @Data
    public static class DolphinSQLFieldDomain {
        @JsonProperty("field_name")
        private String fieldName;
        @JsonProperty("field_type")
        private String fieldType;
        @JsonProperty("field_length")
        private Integer fieldLength;
        @JsonProperty("field_description")
        private String fieldDescription;
    }
    @Data
    public static class DolphinSQLColumnDomain {
        @JsonProperty("field_name")
        private String fieldName;
        @JsonProperty("field_type")
        private String fieldType;
        @JsonProperty("field_length")
        private Integer fieldLength;
        @JsonProperty("field_description")
        private String fieldDescription;
    }

    @Data
    @TableName("t_lineage_tag_column2")
    public static class DolphinColumnLineage implements Serializable {
        private static final long serialVersionUID = 244947848024328503L;
        @JsonProperty("unique_id")
        @TableField("unique_id")
        private String uniqueId;
        @TableField("column_unique_ids")
        @JsonProperty("column_unique_ids")
        private String columnUniqueIds;
        @TableField("expression_name")
        @JsonProperty("expression_name")
        private String expressionName;
        // 将依赖的表的cat schema table 放入tabDepSet，后面用于请求元数据信息放入图谱中
        @JsonIgnore
        private HashSet<String> tabDepSet = new HashSet<>();
    }
    @Data
    public static class DolphinColumnLineageSuper extends DolphinColumnLineage {
        @JsonProperty(value = "table_unique_id")
        private String tableUniqueId;
        private String uuid;
        private String comment;
        @JsonProperty(value = "data_type")
        private String dataType;
        @JsonProperty(value = "primary_key")
        private String primaryKey;
    }

}
