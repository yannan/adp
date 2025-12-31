package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class TableItemVo implements Serializable {
    @ApiModelProperty(value = "数据源类型", example = "", dataType = "java.lang.Integer")
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源类型名称", example = "", dataType = "java.lang.String")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "数据源ID", example = "", dataType = "java.lang.Long")
    private Long dataSourceId;

    @ApiModelProperty(value = "数据源名称", example = "", dataType = "java.lang.String")
    private String dataSourceName;

    @ApiModelProperty(value = "schema ID", example = "", dataType = "java.lang.Long")
    private Long schemaId;

    @ApiModelProperty(value = "schema名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String schemaName;

    @ApiModelProperty(value = "主键", example = "", dataType = "java.lang.Long")
    private Long id;

    @ApiModelProperty(value = "表名称", example = "t_test", dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(value = "表描述", example = "", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(value = "高级参数", example = "", dataType = "java.lang.String")
    private String advancedParams;

    @ApiModelProperty(value = "创建时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "创建时间戳", example = "", dataType = "java.lang.Long")
    private Long createTimeStamp;

    @ApiModelProperty(value = "修改时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @ApiModelProperty(value = "修改时间戳", example = "", dataType = "java.lang.Long")
    private Long updateTimeStamp;

    @ApiModelProperty(value = "行数", example = "111", dataType = "java.lang.Long")
    private Long tableRows;

    @ApiModelProperty(value = "是否有字段", example = "false", dataType = "java.lang.Boolean")
    private Boolean haveField;


    public Long getCreateTimeStamp(){
        return this.createTime.getTime();
    }

    public Long getUpdateTimeStamp(){
        return this.updateTime.getTime();
    }
}
