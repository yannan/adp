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
public enum DataSourceUpdateStatusEnum {
    IGNORE(0,"无需更新"),
    WAITING(1,"待更新"),
    UPDATING(2 , "更新中"),
    UNAVAILABLE(3,"连接不可用"),
    UNAUTHORIZED(4,"无权限"),
    BROADCASTING(5,"待广播"),
    ;

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
