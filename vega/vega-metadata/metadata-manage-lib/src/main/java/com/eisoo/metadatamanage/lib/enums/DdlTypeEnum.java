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
public enum DdlTypeEnum {
    AlterTable(0,"修改表"),
    AlterColumn(1, "修改字段"),
    CreateTable(2,"建表"),
    CommentTable(3 , "注释表"),
    CommentColumn(4,"注释字段"),
    DropTable(5, "删除表"),
    RenameTable(6,"重命名表");

    @EnumValue
    @JsonValue
    private Integer code;
    private String message;
}
