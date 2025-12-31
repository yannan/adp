package com.eisoo.metadatamanage.web.extra.model;

import lombok.Data;

import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.model
 * @Date: 2023/3/31 9:23
 */
@Data
public class ConnectionParamObject {
    private String user;

    private String password;

    private String address;

    private String database;

    private String jdbcUrl;

    private String driverLocation;

    private String driverClassName;

    private String validationQuery;

    private String other;

    private Map<String, String> props;

}
