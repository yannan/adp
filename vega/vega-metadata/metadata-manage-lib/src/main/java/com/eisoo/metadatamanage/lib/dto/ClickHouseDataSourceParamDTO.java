package com.eisoo.metadatamanage.lib.dto;


import com.eisoo.metadatamanage.lib.enums.DbType;

public class ClickHouseDataSourceParamDTO extends BaseDataSourceParamDTO {

    @Override
    public String toString() {
        return "ClickHouseDataSourceParamDTO{"
                + "host='" + host + '\''
                + ", port=" + port
                + ", database='" + database + '\''
                + ", userName='" + userName + '\''
                + ", password='" + password + '\''
                + ", other='" + other + '\''
                + '}';
    }

    @Override
    public DbType getType() {
        return DbType.CLICKHOUSE;
    }
}
