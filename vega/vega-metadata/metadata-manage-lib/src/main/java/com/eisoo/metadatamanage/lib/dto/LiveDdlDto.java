package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DdlAffectEnum;
import com.eisoo.metadatamanage.lib.enums.DdlTypeEnum;
import com.eisoo.metadatamanage.lib.enums.DdlUpdateStatusEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class LiveDdlDto {
    Long id;
    String originCatalog;
    String virtualCatalog;
    Long schemaId;
    String schemaName;
    String dataSourceId;
    String dataSourceName;
    Integer datasourceType;
    String datasourceTypeName;
    Long tableId;
    String tableName;
    String targetTable;
    List<String> columns;
    String statement;
    DdlTypeEnum type;
    String comment;
    DdlAffectEnum affect;
    Date monitorTime;
    DdlUpdateStatusEnum updateStatus;
    String updateMessage;

}
