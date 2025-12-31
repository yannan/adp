package com.eisoo.metadatamanage.lib.vo;

import com.eisoo.metadatamanage.lib.dto.TableFieldSearchDto;
import lombok.Data;

import java.util.List;

@Data
public class TableFieldSearchVo extends TableFieldSearchDto {

    String dsName;

    List<Field> fields;

    @Data
    public static class Field {
        String fieldName;
        String fieldType;
        Boolean primaryKey;
        String originFieldType;
        String virtualFieldType;
    }
}
