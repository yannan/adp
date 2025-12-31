package com.eisoo.metadatamanage.lib.enums;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.enums
 * @Date: 2023/4/10 10:04
 */
public enum DbConnectType {
    ORACLE_SERVICE_NAME(0, "Oracle Service Name"),
    ORACLE_SID(1, "Oracle SID");

    DbConnectType(int code, String descp) {
        this.code = code;
        this.descp = descp;
    }

    private final int code;

    private final String descp;

    public int getCode() {
        return code;
    }

    public String getDescp() {
        return descp;
    }
}
