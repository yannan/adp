package com.eisoo.metadatamanage.lib.vo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class SchemaCatagoryItemVo implements Serializable {
    @JsonIgnore
    private Integer dataSourceType;
    
    @JsonIgnore
    private String dataSourceId;

    @ApiModelProperty(value = "schema ID", example = "", dataType = "java.lang.Long")
    private Long id;

    @ApiModelProperty(value = "schema名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    private String name;
}
