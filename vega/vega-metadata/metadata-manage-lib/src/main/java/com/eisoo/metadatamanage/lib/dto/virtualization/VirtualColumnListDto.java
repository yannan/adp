package com.eisoo.metadatamanage.lib.dto.virtualization;

import lombok.Data;

import java.util.List;

@Data
public class VirtualColumnListDto {
    List<VirtualColumnDto> data;
    Integer totalCount;

    @Data
    public static class VirtualColumnDto {
        String name;
        String type;
        String origType;
        Boolean primaryKey;
        Boolean nullAble;
        String columnDef;
        String comment;
        TypeSignature typeSignature;
        @Data
        public static class TypeSignature {
            String rawType;
            List<Arguments> arguments;
            @Data
            public static class Arguments {
                String kind;
                Integer value;
            }
        }
    }
}
