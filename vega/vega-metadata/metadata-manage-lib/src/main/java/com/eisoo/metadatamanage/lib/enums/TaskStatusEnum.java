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
public enum TaskStatusEnum {
    SUCCESS(0,"成功"),
    FAIL(1 , "失败"),
    ONGOING(2,"进行中");

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
