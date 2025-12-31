package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.util.Date;

import com.eisoo.metadatamanage.lib.dto.DataSourceDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class DataSourceVo extends DataSourceDTO implements Serializable {
    @ApiModelProperty(value = "数据源类型名称", example = "")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "主键", example = "")
    private Long id;

    @ApiModelProperty(value = "启用状态", example = "")
    private Integer enableStatus;

    @ApiModelProperty(value = "连接状态", example = "")
    private Integer connectStatus;

    /**
     * 权限域（目前为预留字段）
     */
    @ApiModelProperty(value = "权限域（目前为预留字段）", example = "")
    private Long authorityId;
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
}
