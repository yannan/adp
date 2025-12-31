package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.standardization.common.constant.Message;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TableFieldSearchDto {

    @NotNull(message = "ds_id" + Message.MESSAGE_INPUT_NOT_EMPTY)
    Long dsId;

    @NotBlank(message = "db_name" + Message.MESSAGE_INPUT_NOT_EMPTY)
    String dbName;

    @NotBlank(message = "db_schema" + Message.MESSAGE_INPUT_NOT_EMPTY)
    String dbSchema;

    @NotBlank(message = "tb_name" + Message.MESSAGE_INPUT_NOT_EMPTY)
    String tbName;
}
