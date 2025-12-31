package com.eisoo.metadatamanage.lib.dto;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class DataSourceStatusDTO {
    @ApiModelProperty(value = "启用状态", example = "1", dataType = "java.lang.Integer")
    @Range(min = 1, max = 2, message = "启用状态" + Messages.MESSAGE_VALUE_BETWEEN_1_AND_2)
    @NotNull(message = "启用状态" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private Integer enableStatus;
}
