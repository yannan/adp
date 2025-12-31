package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/6/15 17:19
 */
@Data
public class DataSourceKafkaDTO {
    private HeaderDTO header;
    private PayloadDTO payload;
}
