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
public class TableVo extends TableBaseVo implements Serializable {
    @ApiModelProperty(value = "数据源类型", example = "", dataType = "java.lang.Integer")
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源类型名称", example = "", dataType = "java.lang.String")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "数据源ID", example = "", dataType = "java.lang.Long")
    private String dataSourceId;

    @ApiModelProperty(value = "数据源名称", example = "", dataType = "java.lang.String")
    private String dataSourceName;

    @ApiModelProperty(value = "schema ID", example = "", dataType = "java.lang.Long")
    private Long schemaId;

    @ApiModelProperty(value = "schema名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String schemaName;

    @ApiModelProperty(value = "主键", example = "", dataType = "java.lang.Long")
    private Long id;

//    @ApiModelProperty(value = "版本号", example = "V1", dataType = "java.lang.String")
//    private String version;

    /**
     * 权限域（目前为预留字段）
     */
    @ApiModelProperty(value = "权限域（目前为预留字段）", example = "")
    private String authorityId;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", example = "")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 创建用户（ID）
     */
    @ApiModelProperty(value = "创建用户（ID）", example = "")
    private String createUser;
    /**
     * 修改时间
     */
    @ApiModelProperty(value = "修改时间", example = "")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 修改用户（ID）
     */
    @ApiModelProperty(value = "修改用户（ID）", example = "")
    private String updateUser;

    @ApiModelProperty(value = "创建时间戳", example = "", dataType = "java.lang.Long")
    private Long createTimeStamp;

    @ApiModelProperty(value = "修改时间戳", example = "", dataType = "java.lang.Long")
    private Long updateTimeStamp;

    public Long getCreateTimeStamp(){
        return this.createTime.getTime();
    }

    public Long getUpdateTimeStamp(){
        return this.updateTime.getTime();
    }
}
