package com.eisoo.metadatamanage.web.extra.model;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/4/1 16:34
 */
public class HiveConnectionParam extends BaseHDFSConnectionParam {
    @Override
    public String toString() {
        return "HiveConnectionParam{"
                + "user='" + user + '\''
                + ", password='" + password + '\''
                + ", address='" + address + '\''
                + ", database='" + database + '\''
                + ", jdbcUrl='" + jdbcUrl + '\''
                + ", driverLocation='" + driverLocation + '\''
                + ", driverClassName='" + driverClassName + '\''
                + ", validationQuery='" + validationQuery + '\''
                + ", other='" + other + '\''
                + ", principal='" + principal + '\''
                + ", javaSecurityKrb5Conf='" + javaSecurityKrb5Conf + '\''
                + ", loginUserKeytabUsername='" + loginUserKeytabUsername + '\''
                + ", loginUserKeytabPath='" + loginUserKeytabPath + '\''
                + '}';
    }
}
