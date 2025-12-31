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
public enum DdlAffectEnum {
    Column(0,"字段"),
    Table(1, "表"),
    Schema(2,"模式"),
    Catalog(3 , "仓库"),
    TableAndColumn(4,"表与字段");
    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
