package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class DataSourceCatagoryItemVo implements Serializable {
    @JsonIgnore
    private Integer dataSourceType;

    @ApiModelProperty(value = "数据源ID", example = "", dataType = "java.lang.String")
    private String id;

    @ApiModelProperty(value = "数据源名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(value = "schema列表", example = "")
    private List<SchemaCatagoryItemVo> schemas;

    @ApiModelProperty(value = "虚拟化目录名", example = "", dataType = "java.lang.String")
    @JsonProperty(value = "v_catalog_name")
    private String extendProperty;
}
