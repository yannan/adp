package com.eisoo.metadatamanage.lib.dto;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel
public class AdvancedDTO implements Serializable {
    @ApiModelProperty(value = "键", example = "key", dataType = "java.lang.String")
    @Length(max = 128, message = Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "键" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String key;

    @ApiModelProperty(value = "值", example = "value", dataType = "java.lang.String")
    @Length(max = 128, message = Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "值" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String value;
}
