package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

import java.util.Date;

@Data
public class DdlLogDto {
    Long id;
    String catalogName;
    String schemaName;
    String commandTag;
    String statement;
    Date ddlTime;
    String updateMessage;
}
