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
public class TableAlterDTO extends TableCreateDTO {
    @ApiModelProperty(value = "数据源ID", example = "1", dataType = "java.lang.Long")
    @Range(min = 1, max = Long.MAX_VALUE, message = "数据源ID取值范围需满足[1,9223372036854775807]")
    @NotNull(message = "数据源ID" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String dataSourceId;

    @ApiModelProperty(value = "schemaID", example = "1", dataType = "java.lang.Long")
    @Range(min = 1, max = Long.MAX_VALUE, message = "schemaID取值范围需满足[1,9223372036854775807]")
    @NotNull(message = "schemaID" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private Long schemaId;
}
