package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

import java.io.StringBufferInputStream;

@Data
public class DataSourceLiveUpdateStatusDto {
    String datasourceId;
    String command;
    String handleResult;
}