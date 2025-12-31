package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.dto
 * @Date: 2023/6/10 17:12
 */
@Data
public class SchemaRowsDTO {
    private String schemaName;
    private Long schemaRows;
    private Long schemaId;
}
