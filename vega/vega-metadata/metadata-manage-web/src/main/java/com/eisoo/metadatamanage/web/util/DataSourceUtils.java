package com.eisoo.metadatamanage.web.util;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.util
 * @Date: 2023/3/30 15:22
 */
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlRenameTableStatement;
import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.metadatamanage.lib.dto.DdlLogDto;
import com.eisoo.metadatamanage.lib.dto.LiveDdlDto;
import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DdlAffectEnum;
import com.eisoo.metadatamanage.lib.enums.DdlTypeEnum;
import com.eisoo.metadatamanage.lib.enums.DdlUpdateStatusEnum;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.eisoo.metadatamanage.web.provider.DataSourceProcessorProvider;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.lib.enums.DbType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Slf4j
public class DataSourceUtils {
    public DataSourceUtils() {
    }

    private static final Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

    /**
     * check datasource param
     *
     * @param baseDataSourceParamDTO datasource param
     */
    public static void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        getDatasourceProcessor(baseDataSourceParamDTO.getType()).checkDatasourceParam(baseDataSourceParamDTO);
    }

    /**
     * build connection url
     *
     * @param baseDataSourceParamDTO datasourceParam
     */
    public static ConnectionParam buildConnectionParams(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        ConnectionParam connectionParams = getDatasourceProcessor(baseDataSourceParamDTO.getType())
                .createConnectionParams(baseDataSourceParamDTO);
        logger.info("parameters map:{}", connectionParams);
        return connectionParams;
    }

    public static ConnectionParam buildConnectionParams(DbType dbType, String connectionJson) {
        return getDatasourceProcessor(dbType).createConnectionParams(connectionJson);
    }

    public static String getJdbcUrl(DbType dbType, ConnectionParam baseConnectionParam) {
        return getDatasourceProcessor(dbType).getJdbcUrl(baseConnectionParam);
    }

    public static Connection getConnection(DbType dbType, ConnectionParam connectionParam) {
        try {

            return getDatasourceProcessor(dbType).getConnection(connectionParam);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection(DbType dbType, ConnectionParam connectionParam, Boolean throwFlag) throws SQLException, IOException, ClassNotFoundException {
        return getDatasourceProcessor(dbType).getConnection(connectionParam);
    }

    public static String getDatasourceDriver(DbType dbType) {
        return getDatasourceProcessor(dbType).getDatasourceDriver();
    }

    public static BaseDataSourceParamDTO buildDatasourceParamDTO(DbType dbType, String connectionParams) {
        return getDatasourceProcessor(dbType).createDatasourceParamDTO(connectionParams);
    }

    public static DataSourceProcessor getDatasourceProcessor(DbType dbType) {
        Map<String, DataSourceProcessor> dataSourceProcessorMap = DataSourceProcessorProvider.getInstance().getDataSourceProcessorMap();
        System.out.println("DataSourceProcessorProvider.getInstance():"+ DataSourceProcessorProvider.getInstance());
        System.out.println("dataSourceProcessorMap:"+ dataSourceProcessorMap);
        System.out.println("dbType.name():"+dbType.name());
        if (!dataSourceProcessorMap.containsKey(dbType.name())) {
            throw new IllegalArgumentException("illegal datasource type");
        }
        return dataSourceProcessorMap.get(dbType.name());
    }

    /**
     * get datasource UniqueId
     */
    public static String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        return getDatasourceProcessor(dbType).getDatasourceUniqueId(connectionParam, dbType);
    }

    /**
     * build connection url
     */
    public static BaseDataSourceParamDTO buildDatasourceParam(String param) {
        JsonNode jsonNodes = JSONUtils.parseObject(param);

        return getDatasourceProcessor(DbType.ofName(jsonNodes.get("type").asText().toUpperCase()))
                .castDatasourceParamDTO(param);
    }

    public static List<LiveDdlDto> getLiveDdlDto(DdlLogDto ddlLogDto, List<SchemaEntity> schemaEntityList, List<DataSourceEntity> dataSourceEntityList) {
        String sql = ddlLogDto.getStatement();
        Date date = ddlLogDto.getDdlTime();
        List<LiveDdlDto> ddlDtoList = new ArrayList<>();
        if (AiShuUtil.isEmpty(dataSourceEntityList)) {
            log.error("there is not dataSourceEntity when trying to get liveDdlDto");
            return ddlDtoList;
        }
        //ddl特殊处理
        log.info("数据源名称是：{}, 数据源类型是：{}, 原始ddl是：{}", dataSourceEntityList.get(0).getName(), dataSourceEntityList.get(0).getDataSourceTypeName(), sql);
        try {
            String dbtypeName = StringUtils.lowerCase(dataSourceEntityList.get(0).getDataSourceTypeName());
            String originCatalog = dataSourceEntityList.get(0).getDatabaseName();
            Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntityList.get(0).getExtendProperty());
            String virtualCatalog = extendPropertyMap.get(DataSourceConstants.VCATALOGNAME);
            com.alibaba.druid.DbType dbType = com.alibaba.druid.DbType.of(dbtypeName);
            switch (dbType) {
                case postgresql: {
                    sql = StringUtils.trim(sql);
                    sql = sql.replaceAll("\\s+", " ");
                    int usingIndex = StringUtils.indexOfIgnoreCase(sql, "USING");
                    if (usingIndex != -1) {
                        sql = StringUtils.substring(sql, 0, usingIndex);
                    }

                    if (StringUtils.startsWithIgnoreCase(sql, "CREATE")) {
                        int withIndex = StringUtils.indexOfIgnoreCase(sql, "WITH");
                        if (withIndex != -1) {
                            sql = StringUtils.substring(sql, 0, withIndex);
                            int asIndex = StringUtils.indexOfIgnoreCase(sql, " AS");
                            if (asIndex != -1) {
                                sql = StringUtils.substring(sql, 0, asIndex);
                                sql = sql + "(id1 int4)";
                            }
                        }
                    }

                    log.info("转化后的ddl是：{}", sql);

                    List<SQLStatement> statements = SQLUtils.parseStatements(sql, dbType);
                    if (AiShuUtil.isNotEmpty(statements)) {
                        for (SQLStatement s : statements) {
                            if (s instanceof SQLCreateTableStatement) {
                                SQLCreateTableStatement s1 = (SQLCreateTableStatement) s;
                                SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                                String catalog = sqlExprTableSource.getCatalog();
                                String schema = sqlExprTableSource.getSchema();
                                String table = sqlExprTableSource.getTableName();
                                List<String> columns = sqlExprTableSource.getColumns().stream().map(i -> i.getSimpleName()).collect(Collectors.toList());
                                LiveDdlDto liveDdlDto = new LiveDdlDto();
                                liveDdlDto.setType(DdlTypeEnum.CreateTable);
                                liveDdlDto.setOriginCatalog(catalog);
                                if (StringUtils.isEmpty(catalog)) {
                                    liveDdlDto.setOriginCatalog(originCatalog);
                                }
                                liveDdlDto.setVirtualCatalog(virtualCatalog);
                                liveDdlDto.setTableName(table);
                                liveDdlDto.setColumns(columns);
                                liveDdlDto.setStatement(sql);
                                liveDdlDto.setAffect(DdlAffectEnum.TableAndColumn);
                                liveDdlDto.setMonitorTime(date);
                                liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                liveDdlDto.setId(ddlLogDto.getId());
                                ddlDtoList.add(liveDdlDto);
                            }
                            if (s instanceof SQLCommentStatement) {
                                SQLCommentStatement s1 = (SQLCommentStatement) s;
                                SQLCommentStatement.Type type = s1.getType();
                                String typeName = type.name();//TABLE OR COLUMN
                                SQLExprTableSource exprTableSource = s1.getOn();
                                if ("COLUMN".equals(typeName)) {
                                    String columnName = exprTableSource.getName().getSimpleName();
                                    String table;
                                    String schema = null;
                                    String catalog = null;
                                    SQLPropertyExpr columnExpr = (SQLPropertyExpr) exprTableSource.getExpr();
                                    if (columnExpr.getOwner() instanceof SQLPropertyExpr) {
                                        SQLPropertyExpr tableExpr = (SQLPropertyExpr) columnExpr.getOwner();
                                        table = tableExpr.getName();
                                        if (tableExpr.getOwner() instanceof SQLPropertyExpr) {
                                            SQLPropertyExpr schemaExpr = (SQLPropertyExpr) tableExpr.getOwner();
                                            schema = schemaExpr.getName();
                                            if (schemaExpr.getOwner() instanceof SQLIdentifierExpr) {
                                                SQLIdentifierExpr catalogExpr = (SQLIdentifierExpr) schemaExpr.getOwner();
                                                catalog = catalogExpr.getName();
                                            }
                                        } else {
                                            SQLIdentifierExpr schemaExpr = (SQLIdentifierExpr) tableExpr.getOwner();
                                            schema = schemaExpr.getName();
                                        }
                                    } else {
                                        SQLIdentifierExpr tableExpr = (SQLIdentifierExpr) columnExpr.getOwner();
                                        table = tableExpr.getName();
                                    }
                                    LiveDdlDto liveDdlDto = new LiveDdlDto();
                                    liveDdlDto.setOriginCatalog(catalog);
                                    if (StringUtils.isEmpty(catalog)) {
                                        liveDdlDto.setOriginCatalog(originCatalog);
                                    }
                                    liveDdlDto.setVirtualCatalog(virtualCatalog);
                                    liveDdlDto.setTableName(table);
                                    liveDdlDto.setStatement(sql);
                                    SQLCharExpr charExpr = (SQLCharExpr) s1.getComment();
                                    String comment = charExpr.getText();
                                    liveDdlDto.setComment(comment);
                                    List<String> columns = new ArrayList<>();
                                    columns.add(columnName);
                                    liveDdlDto.setType(DdlTypeEnum.CommentColumn);
                                    liveDdlDto.setColumns(columns);
                                    liveDdlDto.setAffect(DdlAffectEnum.Column);
                                    liveDdlDto.setMonitorTime(date);
                                    liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                    liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                    liveDdlDto.setId(ddlLogDto.getId());
                                    ddlDtoList.add(liveDdlDto);
                                } else {
                                    String table = exprTableSource.getTableName();
                                    String schema = exprTableSource.getSchema();
                                    String catalog = exprTableSource.getCatalog();
                                    LiveDdlDto liveDdlDto = new LiveDdlDto();
                                    liveDdlDto.setOriginCatalog(catalog);
                                    if (StringUtils.isEmpty(catalog)) {
                                        liveDdlDto.setOriginCatalog(originCatalog);
                                    }
                                    liveDdlDto.setVirtualCatalog(virtualCatalog);
                                    liveDdlDto.setTableName(table);
                                    liveDdlDto.setStatement(sql);
                                    SQLCharExpr charExpr = (SQLCharExpr) s1.getComment();
                                    String comment = charExpr.getText();
                                    liveDdlDto.setComment(comment);
                                    liveDdlDto.setType(DdlTypeEnum.CommentTable);
                                    liveDdlDto.setAffect(DdlAffectEnum.Table);
                                    liveDdlDto.setMonitorTime(date);
                                    liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                    liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                    liveDdlDto.setId(ddlLogDto.getId());
                                    ddlDtoList.add(liveDdlDto);
                                }
                            }

                            if (s instanceof SQLAlterTableStatement) {
                                SQLAlterTableStatement s1 = (SQLAlterTableStatement) s;
                                SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                                //特殊处理表更名
                                if (AiShuUtil.isNotEmpty(s1.getItems())) {
                                    for (SQLAlterTableItem alterTableItem : s1.getItems()) {
                                        if (alterTableItem instanceof SQLAlterTableRename) {
                                            SQLAlterTableRename renameTable = (SQLAlterTableRename) alterTableItem;
                                            String table = sqlExprTableSource.getTableName();
                                            String target = renameTable.getTo().getTableName();
                                            String schema = sqlExprTableSource.getSchema();
                                            String catalog = sqlExprTableSource.getCatalog();
                                            LiveDdlDto liveDdlDto = new LiveDdlDto();
                                            liveDdlDto.setType(DdlTypeEnum.RenameTable);
                                            liveDdlDto.setOriginCatalog(catalog);
                                            if (StringUtils.isEmpty(catalog)) {
                                                liveDdlDto.setOriginCatalog(originCatalog);
                                            }
                                            liveDdlDto.setVirtualCatalog(virtualCatalog);
                                            liveDdlDto.setTableName(table);
                                            liveDdlDto.setTargetTable(target);
                                            liveDdlDto.setStatement(sql);
                                            liveDdlDto.setAffect(DdlAffectEnum.Table);
                                            liveDdlDto.setMonitorTime(date);
                                            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                            liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                            liveDdlDto.setId(ddlLogDto.getId());
                                            ddlDtoList.add(liveDdlDto);
                                        }
                                        else {
                                            String table = sqlExprTableSource.getTableName();
                                            String schema = sqlExprTableSource.getSchema();
                                            String catalog = sqlExprTableSource.getCatalog();
                                            LiveDdlDto liveDdlDto = new LiveDdlDto();
                                            liveDdlDto.setOriginCatalog(catalog);
                                            if (StringUtils.isEmpty(catalog)) {
                                                liveDdlDto.setOriginCatalog(originCatalog);
                                            }
                                            liveDdlDto.setVirtualCatalog(virtualCatalog);
                                            liveDdlDto.setTableName(table);
                                            liveDdlDto.setStatement(sql);
                                            liveDdlDto.setAffect(DdlAffectEnum.Column);
                                            liveDdlDto.setType(DdlTypeEnum.AlterColumn);
                                            liveDdlDto.setMonitorTime(date);
                                            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                            liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                            liveDdlDto.setId(ddlLogDto.getId());
                                            ddlDtoList.add(liveDdlDto);
                                        }
                                    }
                                }
                            }

                            if (s instanceof SQLDropTableStatement) {
                                SQLDropTableStatement s1 = (SQLDropTableStatement) s;
                                List<SQLExprTableSource> sqlExprTableSources = s1.getTableSources();
                                for (SQLExprTableSource sqlExprTableSource : sqlExprTableSources) {
                                    String table = sqlExprTableSource.getTableName();
                                    String schema = sqlExprTableSource.getSchema();
                                    String catalog = sqlExprTableSource.getCatalog();
                                    LiveDdlDto liveDdlDto = new LiveDdlDto();
                                    liveDdlDto.setOriginCatalog(catalog);
                                    if (StringUtils.isEmpty(catalog)) {
                                        liveDdlDto.setOriginCatalog(originCatalog);
                                    }
                                    liveDdlDto.setVirtualCatalog(virtualCatalog);
                                    liveDdlDto.setTableName(table);
                                    liveDdlDto.setStatement(sql);
                                    liveDdlDto.setType(DdlTypeEnum.DropTable);
                                    liveDdlDto.setAffect(DdlAffectEnum.TableAndColumn);
                                    liveDdlDto.setMonitorTime(date);
                                    liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                    liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                    liveDdlDto.setId(ddlLogDto.getId());
                                    ddlDtoList.add(liveDdlDto);
                                }
                            }
                        }
                    }
                }
                case mysql: {
                    sql = StringUtils.trim(sql);
                    sql = sql.replaceAll("`", "");
                    List<SQLStatement> statements = SQLUtils.parseStatements(sql, dbType);
                    if (AiShuUtil.isNotEmpty(statements)) {
                        for (SQLStatement s : statements) {
                            if (s instanceof SQLCreateTableStatement) {
                                SQLCreateTableStatement s1 = (SQLCreateTableStatement) s;
                                SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                                String catalog = sqlExprTableSource.getCatalog();
                                String schema = sqlExprTableSource.getSchema();
                                String table = sqlExprTableSource.getTableName();
                                List<String> columns = sqlExprTableSource.getColumns().stream().map(i -> i.getSimpleName()).collect(Collectors.toList());
                                LiveDdlDto liveDdlDto = new LiveDdlDto();
                                liveDdlDto.setType(DdlTypeEnum.CreateTable);
                                liveDdlDto.setOriginCatalog(catalog);
                                if (StringUtils.isEmpty(catalog)) {
                                    liveDdlDto.setOriginCatalog(originCatalog);
                                }
                                if (AiShuUtil.isNotEmpty(s1.getComment())) {
                                    SQLCharExpr commentExpr = (SQLCharExpr) s1.getComment();
                                    liveDdlDto.setComment(commentExpr.getText());
                                }
                                liveDdlDto.setVirtualCatalog(virtualCatalog);
                                liveDdlDto.setTableName(table);
                                liveDdlDto.setColumns(columns);
                                liveDdlDto.setStatement(sql);
                                liveDdlDto.setAffect(DdlAffectEnum.TableAndColumn);
                                liveDdlDto.setMonitorTime(date);
                                liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                liveDdlDto.setId(ddlLogDto.getId());
                                ddlDtoList.add(liveDdlDto);
                            }

                            if (s instanceof SQLAlterTableStatement) {
                                SQLAlterTableStatement s1 = (SQLAlterTableStatement) s;
                                SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                                //改表注释
                                List<SQLAssignItem> assignItemList = s1.getTableOptions();
                                if (AiShuUtil.isNotEmpty(assignItemList)) {
                                    for (SQLAssignItem item : assignItemList) {

                                        String table = sqlExprTableSource.getTableName();
                                        String schema = sqlExprTableSource.getSchema();
                                        String catalog = sqlExprTableSource.getCatalog();
                                        LiveDdlDto liveDdlDto = new LiveDdlDto();
                                        if (item.getTarget() instanceof SQLIdentifierExpr) {
                                            SQLIdentifierExpr target = (SQLIdentifierExpr) item.getTarget();
                                            if ("COMMENT".equals(target.getName())) {
                                                SQLCharExpr sqlCharExpr = (SQLCharExpr) item.getValue();
                                                liveDdlDto.setComment(sqlCharExpr.getText());
                                            }
                                        }
                                        liveDdlDto.setOriginCatalog(catalog);
                                        if (StringUtils.isEmpty(catalog)) {
                                            liveDdlDto.setOriginCatalog(originCatalog);
                                        }
                                        liveDdlDto.setVirtualCatalog(virtualCatalog);
                                        liveDdlDto.setTableName(table);
                                        liveDdlDto.setStatement(sql);
                                        liveDdlDto.setType(DdlTypeEnum.CommentTable);
                                        liveDdlDto.setAffect(DdlAffectEnum.Table);
                                        liveDdlDto.setMonitorTime(date);
                                        liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                        liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                        liveDdlDto.setId(ddlLogDto.getId());
                                        ddlDtoList.add(liveDdlDto);
                                    }
                                }
                                //识别字段级改动
                                if (AiShuUtil.isNotEmpty(s1.getItems())) {
                                    String table = sqlExprTableSource.getTableName();
                                    String schema = sqlExprTableSource.getSchema();
                                    String catalog = sqlExprTableSource.getCatalog();
                                    LiveDdlDto liveDdlDto = new LiveDdlDto();
                                    liveDdlDto.setOriginCatalog(catalog);
                                    if (StringUtils.isEmpty(catalog)) {
                                        liveDdlDto.setOriginCatalog(originCatalog);
                                    }
                                    liveDdlDto.setVirtualCatalog(virtualCatalog);
                                    liveDdlDto.setTableName(table);
                                    liveDdlDto.setStatement(sql);
                                    liveDdlDto.setAffect(DdlAffectEnum.Column);
                                    liveDdlDto.setType(DdlTypeEnum.AlterColumn);
                                    liveDdlDto.setMonitorTime(date);
                                    liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                    liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                    liveDdlDto.setId(ddlLogDto.getId());
                                    ddlDtoList.add(liveDdlDto);
                                }
                            }

                            if (s instanceof SQLDropTableStatement) {
                                SQLDropTableStatement s1 = (SQLDropTableStatement) s;
                                List<SQLExprTableSource> sqlExprTableSources = s1.getTableSources();
                                for (SQLExprTableSource sqlExprTableSource : sqlExprTableSources) {
                                    String table = sqlExprTableSource.getTableName();
                                    String schema = sqlExprTableSource.getSchema();
                                    String catalog = sqlExprTableSource.getCatalog();
                                    LiveDdlDto liveDdlDto = new LiveDdlDto();
                                    liveDdlDto.setOriginCatalog(catalog);
                                    if (StringUtils.isEmpty(catalog)) {
                                        liveDdlDto.setOriginCatalog(originCatalog);
                                    }
                                    liveDdlDto.setVirtualCatalog(virtualCatalog);
                                    liveDdlDto.setTableName(table);
                                    liveDdlDto.setStatement(sql);
                                    liveDdlDto.setType(DdlTypeEnum.DropTable);
                                    liveDdlDto.setAffect(DdlAffectEnum.TableAndColumn);
                                    liveDdlDto.setMonitorTime(date);
                                    liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                    liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                    liveDdlDto.setId(ddlLogDto.getId());
                                    ddlDtoList.add(liveDdlDto);
                                }
                            }

                            //修改表名
                            if (s instanceof MySqlRenameTableStatement) {
                                MySqlRenameTableStatement  s1 = (MySqlRenameTableStatement) s;
                                List<MySqlRenameTableStatement.Item> itemList = s1.getItems();
                                if (AiShuUtil.isNotEmpty(itemList)) {
                                    for (MySqlRenameTableStatement.Item item : itemList) {
                                        if (item.getName() instanceof SQLIdentifierExpr) {
                                            SQLIdentifierExpr oldExpr = (SQLIdentifierExpr) item.getName();
                                            String oldName = oldExpr.getName();
                                            SQLIdentifierExpr newExpr = (SQLIdentifierExpr) item.getTo();
                                            String newName = newExpr.getName();
                                            String table = oldName;
                                            String target = newName;
                                            String schema = null;
                                            String catalog = null;
                                            LiveDdlDto liveDdlDto = new LiveDdlDto();
                                            liveDdlDto.setType(DdlTypeEnum.RenameTable);
                                            liveDdlDto.setOriginCatalog(catalog);
                                            if (StringUtils.isEmpty(catalog)) {
                                                liveDdlDto.setOriginCatalog(originCatalog);
                                            }
                                            liveDdlDto.setVirtualCatalog(virtualCatalog);
                                            liveDdlDto.setTableName(table);
                                            liveDdlDto.setTargetTable(target);
                                            liveDdlDto.setStatement(sql);
                                            liveDdlDto.setAffect(DdlAffectEnum.Table);
                                            liveDdlDto.setMonitorTime(date);
                                            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                            liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                            liveDdlDto.setId(ddlLogDto.getId());
                                            ddlDtoList.add(liveDdlDto);
                                        }
                                        if (item.getName() instanceof SQLPropertyExpr) {
                                            SQLPropertyExpr oldExpr = (SQLPropertyExpr) item.getName();
                                            String oldName = oldExpr.getName();
                                            SQLIdentifierExpr oldOwner = (SQLIdentifierExpr) oldExpr.getOwner();
                                            String schema = oldOwner.getName();
                                            SQLPropertyExpr newExpr = (SQLPropertyExpr) item.getTo();
                                            String newName = newExpr.getName();
                                            String table = oldName;
                                            String target = newName;
                                            String catalog = schema;
                                            LiveDdlDto liveDdlDto = new LiveDdlDto();
                                            liveDdlDto.setType(DdlTypeEnum.RenameTable);
                                            liveDdlDto.setOriginCatalog(catalog);
                                            if (StringUtils.isEmpty(catalog)) {
                                                liveDdlDto.setOriginCatalog(originCatalog);
                                            }
                                            liveDdlDto.setVirtualCatalog(virtualCatalog);
                                            liveDdlDto.setTableName(table);
                                            liveDdlDto.setTargetTable(target);
                                            liveDdlDto.setStatement(sql);
                                            liveDdlDto.setAffect(DdlAffectEnum.Table);
                                            liveDdlDto.setMonitorTime(date);
                                            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_WAITING);
                                            liveDdlDto = addSchemaAndDataSource(liveDdlDto, ddlLogDto, schema, schemaEntityList, dataSourceEntityList);
                                            liveDdlDto.setId(ddlLogDto.getId());
                                            ddlDtoList.add(liveDdlDto);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
        } catch (Exception e) {
            log.error("实时监听解析ddl{}失败,错误信息{}", sql, e.toString());
            LiveDdlDto liveDdlDto = new LiveDdlDto();
            liveDdlDto.setStatement(sql);
            liveDdlDto.setMonitorTime(date);
            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.PARSE_FAIL);
            liveDdlDto.setUpdateMessage(e.toString());
            liveDdlDto.setId(ddlLogDto.getId());
            ddlDtoList.add(liveDdlDto);
        }

        return ddlDtoList;
    }

    private static DataSourceEntity getDataSourceEntityBySchemaName(String schemaName, List<DataSourceEntity> dataSourceEntityList) {
        if (AiShuUtil.isEmpty(dataSourceEntityList) || StringUtils.isEmpty(schemaName)) {
            return null;
        } else {
            return dataSourceEntityList.stream().filter(ds -> {
                Map<String, String> extendPropertyMap = JSONUtils.props2Map(ds.getExtendProperty());
                String dsSchema = extendPropertyMap.get(DataSourceConstants.SCHEMAKEY);
                return dsSchema.equals(schemaName);
            }).findAny().orElse(null);
        }
    }

    private static SchemaEntity getSchemaEntityBySchemaName(String schemaName, List<SchemaEntity> schemaEntityList, List<DataSourceEntity> dataSourceEntityList) {
        if (AiShuUtil.isEmpty(schemaEntityList) || AiShuUtil.isEmpty(dataSourceEntityList) || StringUtils.isEmpty(schemaName)) {
            return null;
        } else {
            DataSourceEntity dataSourceEntity = getDataSourceEntityBySchemaName(schemaName, dataSourceEntityList);
            SchemaEntity schema = null;
            if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
                schema = schemaEntityList.stream().filter(schemaEntity -> schemaEntity.getDataSourceId().equals(dataSourceEntity.getId())).findAny().orElse(null);
            }
            return schema;
        }
    }

    private static LiveDdlDto addSchemaAndDataSource(LiveDdlDto liveDdlDto, DdlLogDto ddlLogDto, String schemaName, List<SchemaEntity> schemaEntityList, List<DataSourceEntity> dataSourceEntityList) {
        log.info("校验ddl的schema，LiveDdlDto：{}，ddlLogDto：{}, schemaName:{}", liveDdlDto, ddlLogDto, schemaName);
        if (StringUtils.isEmpty(schemaName) && StringUtils.isNotEmpty(ddlLogDto.getSchemaName())
                && !ddlLogDto.getUpdateMessage().equals("queryEventData.getDatabase():" + (StringUtils.isEmpty(ddlLogDto.getSchemaName()) ? "empty" : ddlLogDto.getSchemaName()))) {
            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
            log.info("空schemaName校验结果：{}", StringUtils.isEmpty(schemaName) && StringUtils.isEmpty(ddlLogDto.getSchemaName())?"抛弃":"执行");
            return liveDdlDto;
        }

        if (StringUtils.isNotEmpty(schemaName) && !schemaName.equals(ddlLogDto.getSchemaName())) {
            liveDdlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
            log.info("schemaName:{}", schemaName);
            log.info("ddlLogDto.getSchemaName():{}", ddlLogDto.getSchemaName());
            log.info("非空schemaName校验结果：{}", StringUtils.isNotEmpty(schemaName) && !schemaName.equals(ddlLogDto.getSchemaName())?"抛弃":"执行");
            return liveDdlDto;
        }

        if (StringUtils.isNotEmpty(ddlLogDto.getSchemaName()) && StringUtils.isEmpty(schemaName)) {
            schemaName = ddlLogDto.getSchemaName();
        }

        SchemaEntity schemaEntity = getSchemaEntityBySchemaName(schemaName, schemaEntityList, dataSourceEntityList);
        if (AiShuUtil.isNotEmpty(schemaEntity)) {
            liveDdlDto.setSchemaId(schemaEntity.getId());
            liveDdlDto.setSchemaName(schemaEntity.getName());
        }
        DataSourceEntity dataSourceEntity = getDataSourceEntityBySchemaName(schemaName, dataSourceEntityList);
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            liveDdlDto.setDataSourceId(dataSourceEntity.getId());
            liveDdlDto.setDatasourceType(dataSourceEntity.getDataSourceType());
            liveDdlDto.setDataSourceName(dataSourceEntity.getName());
            liveDdlDto.setDatasourceTypeName(dataSourceEntity.getDataSourceTypeName());
        }
        return liveDdlDto;
    }
}
