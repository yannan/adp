package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class CatagoryItemVo implements Serializable {
    @ApiModelProperty(value = "数据源类型", example = "", dataType = "java.lang.Integer")
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源类型名称", example = "", dataType = "java.lang.String")
    private String dataSourceTypeName;

    @ApiModelProperty(value = "数据源列表", example = "")
    private List<DataSourceCatagoryItemVo> dataSources;
}
