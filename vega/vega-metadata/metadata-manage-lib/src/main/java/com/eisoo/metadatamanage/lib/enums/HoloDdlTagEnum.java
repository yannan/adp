package com.eisoo.metadatamanage.lib.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HoloDdlTagEnum {
    ALTER_TABLE(0, "ALTER TABLE"),
    COMMENT(1, "COMMENT"),
    CREATE_TABLE(2, "CREATE TABLE"),
    CREATE_FOREIGN_TABLE(3, "CREATE FOREIGN TABLE"),
    CREATE_TABLE_AS(3, "CREATE TABLE AS"),
    DROP_TABLE(4, "DROP TABLE"),
    DROP_FOREIGN_TABLE(5, "DROP FOREIGN TABLE")
    ;
    @EnumValue
    @JsonValue
    private Integer code;
    private String message;

    public static HoloDdlTagEnum of(String name) {
        for (HoloDdlTagEnum c : HoloDdlTagEnum.values()) {
            if (c.getMessage().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }
}
