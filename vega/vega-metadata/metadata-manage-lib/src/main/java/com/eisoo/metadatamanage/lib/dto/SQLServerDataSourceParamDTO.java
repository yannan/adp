package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DbType;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/5/5 14:46
 */
public class SQLServerDataSourceParamDTO extends BaseDataSourceParamDTO {

    @Override
    public String toString() {
        return "SqlServerDataSourceParamDTO{"
                + "name='" + name + '\''
                + ", note='" + note + '\''
                + ", host='" + host + '\''
                + ", port=" + port
                + ", database='" + database + '\''
                + ", userName='" + userName + '\''
                + ", password='" + password + '\''
                + ", other='" + other + '\''
                + '}';
    }

    @Override
    public DbType getType() {
        return DbType.SQLSERVER;
    }
}