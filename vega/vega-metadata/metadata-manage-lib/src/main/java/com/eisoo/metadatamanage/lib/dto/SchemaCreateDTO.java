package com.eisoo.metadatamanage.lib.dto;

import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class SchemaCreateDTO {
    @ApiModelProperty(value = "数据库名称", example = "dbname", dataType = "java.lang.String")
    @Length(max = 128, message = "数据库名称" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "数据库名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String name;
}
