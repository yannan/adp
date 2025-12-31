package com.eisoo.metadatamanage.web.extra.model;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/4/12 10:10
 */
public class PostgreSQLConnectionParam extends BaseConnectionParam {
    @Override
    public String toString() {
        return "PostgreSQLConnectionParam{"
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
