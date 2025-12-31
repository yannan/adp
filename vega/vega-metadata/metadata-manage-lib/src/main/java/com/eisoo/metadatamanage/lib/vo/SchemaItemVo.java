package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.eisoo.metadatamanage.lib.dto.SchemaAlterDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class SchemaItemVo extends SchemaAlterDTO implements Serializable {
    @ApiModelProperty(value = "数据源类型", example = "", dataType = "java.lang.Integer")
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源类型名称", example = "", dataType = "java.lang.String")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "数据源ID", example = "", dataType = "java.lang.Long")
    private Long dataSourceId;

    @ApiModelProperty(value = "数据源名称", example = "", dataType = "java.lang.String")
    private String dataSourceName;

    @ApiModelProperty(value = "主键", example = "", dataType = "java.lang.Long")
    private Long id;

    @ApiModelProperty(value = "名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(value = "创建时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "修改时间", example = "", dataType = "java.util.Date")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @ApiModelProperty(value = "虚拟化目录名", example = "", dataType = "java.lang.String")
    @JsonProperty(value = "v_catalog_name")
    private String extendProperty;
}
