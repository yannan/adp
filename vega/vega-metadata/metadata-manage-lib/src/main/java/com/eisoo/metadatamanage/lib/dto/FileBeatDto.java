package com.eisoo.metadatamanage.lib.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileBeatDto {
    @JsonProperty("@timestamp")
    private String timestamp;
    private String message;
    private Fields fields;

    @Data
    public static class Fields {
        String datasource;
    }
}
