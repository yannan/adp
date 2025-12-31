package com.eisoo.metadatamanage.web.extra.model;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/3/31 14:58
 */
public class MySQLConnectionParam extends BaseConnectionParam {
    @Override
    public String toString() {
        return "MySQLConnectionParam{"
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
