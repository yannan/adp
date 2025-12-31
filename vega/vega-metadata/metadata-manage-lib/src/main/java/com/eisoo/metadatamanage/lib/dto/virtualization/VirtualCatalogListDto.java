package com.eisoo.metadatamanage.lib.dto.virtualization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VirtualCatalogListDto {
    List<VirtualCatalogDto> entries;
    Integer totalCount;

    @Data
    public static class VirtualCatalogDto {
        @JsonProperty(value = "catalog_name")
        String catalogName;
        @JsonProperty(value = "name")
        String connectorName;
    }
}
