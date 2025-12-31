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
public enum DdlPushStatusEnum {
    PUSH_IGNORE(0,"不推送"),
    PUSH_WAITING(1,"待推送"),
    PUSH_FINISH(2,"已推送"),
    ;

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
