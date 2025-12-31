package com.eisoo.metadatamanage.web.extra.model;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/5/5 14:53
 */
public class SQLServerConnectionParam extends BaseConnectionParam {
    @Override
    public String toString() {
        return "SQLServerConnectionParam{"
                + "user='" + user + '\''
                + ", password='" + password + '\''
                + ", address='" + address + '\''
                + ", database='" + database + '\''
                + ", jdbcUrl='" + jdbcUrl + '\''
                + ", driverLocation='" + driverLocation + '\''
                + ", driverClassName='" + driverClassName + '\''
                + ", validationQuery='" + validationQuery + '\''
                + ", other='" + other + '\''
                + '}';
    }
}