package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DbType;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/4/12 10:08
 */
public class PostgreSQLDataSourceParamDTO extends BaseDataSourceParamDTO{
    @Override
    public String toString() {
        return "PostgreSQLDataSourceParamDTO{"
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
        return DbType.POSTGRESQL;
    }
}
