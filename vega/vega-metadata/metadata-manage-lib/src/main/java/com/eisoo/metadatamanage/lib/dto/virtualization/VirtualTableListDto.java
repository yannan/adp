package com.eisoo.metadatamanage.lib.dto.virtualization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VirtualTableListDto {
    List<VirtualTableDto> data;
    Integer totalCount;

    @Data
    public static class VirtualTableDto {
        String catalog;
        String schema;
        String table;
        String fqn;
        String primaryKeyName;
        String comment;
        @JsonProperty(value = "tableType")
        String tableType;
        Object params;
    }
}
