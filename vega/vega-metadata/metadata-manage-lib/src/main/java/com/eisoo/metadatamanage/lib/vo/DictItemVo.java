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
public class DictItemVo implements Serializable {
    @JsonIgnore
    private Integer dictType;

    @ApiModelProperty(value = "字典枚举", example = "", dataType = "java.lang.Integer")
    private Integer dictKey;

    @ApiModelProperty(value = "枚举说明", example = "", dataType = "java.lang.String")
    private String dictValue;
}
