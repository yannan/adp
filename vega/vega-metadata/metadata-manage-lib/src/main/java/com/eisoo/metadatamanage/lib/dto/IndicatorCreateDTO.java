package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.util.constant.Messages;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.dto
 * @Date: 2023/6/12 9:57
 */
@Data
@ApiModel
public class IndicatorCreateDTO {
    @ApiModelProperty(value = "指标名称", example = "name", dataType = "java.lang.String")
    @Length(max = 128, message = "指标名称" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "表名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String name;

    @ApiModelProperty(value = "指标类型", example = "type", dataType = "java.lang.String")
    @Length(max = 128, message = "指标类型" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "指标类型" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String type;

    @ApiModelProperty(value = "指标数值", example = "111111", dataType = "java.lang.Long")
    @Range(min = 1, max = Long.MAX_VALUE, message = "数据源ID取值范围需满足[1,9223372036854775807]")
    @NotNull(message = "指标数值" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private Long value;
}
