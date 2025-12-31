package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.standardization.common.constant.Message;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineageReportDto {
    @ApiModelProperty(value = "血缘")
    @Valid
    List<Lineage> data;

    @Data
    public static class Lineage {
        @ApiModelProperty(value = "源端节点")
        @NotNull(message = "source" + Message.MESSAGE_INPUT_NOT_EMPTY)
        @Valid
        Column source;

        @ApiModelProperty(value = "目的端节点")
        @NotNull(message = "target" + Message.MESSAGE_INPUT_NOT_EMPTY)
        @Valid
        Column target;

        @ApiModelProperty(value = "产生血缘的语句（hive：sql）", dataType = "java.lang.String")
        String queryText;

        @Data
        public static class Column {
            @ApiModelProperty(value = "数据库类型", dataType = "java.lang.String")
            @NotBlank(message = "db_type;" + Message.MESSAGE_INPUT_NOT_EMPTY)
            String dbType;

            @ApiModelProperty(value = "数据源")
            @Valid
            DataSource dataSource;

            @ApiModelProperty(value = "数据库名称", dataType = "java.lang.String")
            String dbName;

            @ApiModelProperty(value = "数据库schema", dataType = "java.lang.String")
            String dbSchema;

            @ApiModelProperty(value = "表名称", dataType = "java.lang.String")
            @NotBlank(message = "tb_name;" + Message.MESSAGE_INPUT_NOT_EMPTY)
            String tbName;

            @ApiModelProperty(value = "字段名称", dataType = "java.lang.String")
            String column;
        }

        @Data
        public static class DataSource {
            @ApiModelProperty(value = "数据数据源ID", dataType = "java.lang.String")
            @NotNull(message = "ds_id" + Message.MESSAGE_INPUT_NOT_EMPTY)
            Long dsId;

            @ApiModelProperty(value = "数据源链接", dataType = "java.lang.String")
            String jdbcUrl;

            @ApiModelProperty(value = "数据源用户名", dataType = "java.lang.String")
            String jdbcUser;
        }
    }

}
