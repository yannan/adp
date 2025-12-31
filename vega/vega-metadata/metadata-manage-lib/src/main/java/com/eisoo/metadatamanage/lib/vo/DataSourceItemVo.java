package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class DataSourceItemVo implements Serializable{
    @ApiModelProperty(value = "类型", example = "3306", dataType = "java.lang.Integer")
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源类型名称", example = "", dataType = "java.lang.String")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "主键", example = "", dataType = "java.lang.String")
    private String id;

    @ApiModelProperty(value = "名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(value = "描述", example = "", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(value = "配置信息", example = "", dataType = "java.lang.String")
    private String config;

    @ApiModelProperty(value = "启用状态", example = "1", dataType = "java.lang.Integer")
    private Integer enableStatus;

    @ApiModelProperty(value = "连接状态", example = "1", dataType = "java.lang.Integer")
    private Integer connectStatus;

    @ApiModelProperty(value = "创建时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "修改时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
