package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.dto
 * @Date: 2023/6/10 17:12
 */
@Data
public class DataSourceRowsDTO {
    private String dataSourceName;
    private Long dataSourceRows;
    private Long dataSourceId;
}
