package com.eisoo.metadatamanage.web.extra.model;

import com.eisoo.metadatamanage.lib.enums.DbConnectType;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/4/10 9:54
 */
public class OracleConnectionParam extends BaseConnectionParam{
    protected DbConnectType connectType;

    public DbConnectType getConnectType() {
        return connectType;
    }

    public void setConnectType(DbConnectType connectType) {
        this.connectType = connectType;
    }

    @Override
    public String toString() {
        return "OracleConnectionParam{"
                + "user='" + user + '\''
                + ", password='" + password + '\''
                + ", address='" + address + '\''
                + ", database='" + database + '\''
                + ", jdbcUrl='" + jdbcUrl + '\''
                + ", driverLocation='" + driverLocation + '\''
                + ", driverClassName='" + driverClassName + '\''
                + ", validationQuery='" + validationQuery + '\''
                + ", other='" + other + '\''
                + ", connectType=" + connectType
                + '}';
    }
}
