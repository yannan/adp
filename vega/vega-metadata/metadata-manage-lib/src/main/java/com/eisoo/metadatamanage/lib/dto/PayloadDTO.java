package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.other
 * @Date: 2023/6/15 17:51
 */
@Data
public class PayloadDTO {
    private Long data_source_id;
    private String id;
    private String infoSystemId;
    private String name;
    private String catalog_name;
    private Integer type;
    private String type_name;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String database_name;
    private String schema;
    private String guardian_token;
}
