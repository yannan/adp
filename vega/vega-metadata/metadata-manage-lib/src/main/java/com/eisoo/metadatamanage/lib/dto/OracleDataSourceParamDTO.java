package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DbConnectType;
import com.eisoo.metadatamanage.lib.enums.DbType;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/4/10 10:08
 */
public class OracleDataSourceParamDTO extends BaseDataSourceParamDTO{
    private DbConnectType connectType;

    public DbConnectType getConnectType() {
        return connectType;
    }

    public void setConnectType(DbConnectType connectType) {
        this.connectType = connectType;
    }

    @Override
    public String toString() {
        return "OracleDataSourceParamDTO{"
                + "name='" + name + '\''
                + ", note='" + note + '\''
                + ", host='" + host + '\''
                + ", port=" + port
                + ", database='" + database + '\''
                + ", userName='" + userName + '\''
                + ", password='" + password + '\''
                + ", connectType=" + connectType
                + ", other='" + other + '\''
                + '}';
    }

    @Override
    public DbType getType() {
        return DbType.ORACLE;
    }
}
