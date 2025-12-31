package com.eisoo.metadatamanage.lib.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/7/4 16:00
 */
@Data
public class DataSourceDolphinDTO {
    private String type;
    private String name;
    private String note;
    private String host;
    private Integer port;
    private String principal;
    private String javaSecurityKrb5Conf;
    private String loginUserKeytabUsername;
    private String loginUserKeytabPath;
    private String userName;
    private String password;
    private String database;
    private String connectType;
    private JsonNode other;
    private Integer id;
}
