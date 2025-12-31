package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DbType;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/4/1 16:31
 */
public class HiveDataSourceParamDTO extends BaseHDFSDataSourceParamDTO {
    @Override
    public String toString() {
        return "HiveDataSourceParamDTO{"
                + "host='" + host + '\''
                + ", port=" + port
                + ", database='" + database + '\''
                + ", principal='" + principal + '\''
                + ", userName='" + userName + '\''
                + ", password='" + password + '\''
                + ", other='" + other + '\''
                + ", javaSecurityKrb5Conf='" + javaSecurityKrb5Conf + '\''
                + ", loginUserKeytabUsername='" + loginUserKeytabUsername + '\''
                + ", loginUserKeytabPath='" + loginUserKeytabPath + '\''
                + '}';
    }

    @Override
    public DbType getType() {
        return DbType.HIVE;
    }
}
