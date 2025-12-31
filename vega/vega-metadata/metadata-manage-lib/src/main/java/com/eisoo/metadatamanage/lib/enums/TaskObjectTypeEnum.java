package com.eisoo.metadatamanage.lib.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.enums
 * @Date: 2023/5/19 11:01
 */
@Getter
@AllArgsConstructor
public enum TaskObjectTypeEnum {
    Unknown(0,"未知"),
    DATASOURCE(1 , "数据源"),
    TABLE(2,"数据表"),
    STOP_TASK(3,"中止任务"),
    UPDATE_PASSWORD(4,"更新密码"),
    START_BINLOG(5,"启动Binlog");

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
