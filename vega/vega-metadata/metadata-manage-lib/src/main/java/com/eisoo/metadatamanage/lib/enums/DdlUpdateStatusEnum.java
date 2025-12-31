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
public enum DdlUpdateStatusEnum {
    UPDATE_ALL(0 , "全量更新"),
    UPDATE_INC(1,"增量更新"),
    UPDATE_IGNORE(2,"忽略更新"),
    UPDATE_WAITING(3,"待更新"),
    PARSE_FAIL(4,"解析失败"),
    UPDATE_FAIL(5,"更新失败"),
    ;

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
