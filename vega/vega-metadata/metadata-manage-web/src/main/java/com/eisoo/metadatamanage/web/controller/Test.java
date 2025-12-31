package com.eisoo.metadatamanage.web.controller;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlRenameTableStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eisoo.metadatamanage.db.entity.TaskEntity;
import com.eisoo.metadatamanage.lib.dto.LiveDdlDto;
import com.eisoo.metadatamanage.lib.enums.*;
import com.eisoo.metadatamanage.web.service.ITaskService;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.eisoo.standardization.common.util.ConvertUtil;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.NullEventDataDeserializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/26 14:44
 * @Version:1.0
 */
@RestController
@RequestMapping("/v1/build")
@Slf4j
@Api(tags = "测试")
public class Test {

    @Autowired
    ITaskService taskService;
    private Map<Long, BinaryLogClient> binaryLogClientMap = new HashMap<>();
//    @ResponseBody
//    @PutMapping(value = "/testDolphin")
//    @ApiOperation(value = "01.testDolphin", notes = "dolphin同步sql测试")
//    public Result<?> testDolphin(@RequestParam(value = "timePara", required = false) String timePara) throws Exception {
////        dolphinSqlConsumerStreamService.getSQLFromDolphinConsumerStream(timePara);
//        return Result.success(PasswordUtils.encodePasswordRSA("eisoo.com123"));
//    }

    @ApiOperation(value = "02.ddl解析测试", notes = "ddl解析测试")
    @PostMapping(value = "/ddlTest")
    public Result<?> ddlTest(
            @Validated @RequestBody String sql
    ) {
//        sql = StringUtils.substringAfter(sql, "CST");
//        sql = StringUtils.substringAfter(sql, ":");
//        sql = StringUtils.substringAfter(sql, ":");
//        int usingIndex = StringUtils.indexOfIgnoreCase(sql,"USING");
//        if (usingIndex != -1) {
//            sql = StringUtils.substring(sql, 0, usingIndex);
//        }
//        sql = StringUtils.trim(sql);
//        sql = StringUtils.replace(sql, "\\t", " ");
//        sql = StringUtils.replace(sql, "\\n", " ");


//        Statement statement = new SqlParser().createStatement(sql, new ParsingOptions(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL));
//        List<NativeQuery> queries = Parser.parseJdbcSql(sql, true, false, true, true, true, new String[0]);
//        PGStream pgStream = new PGStream(SocketFactory.getDefault(), new HostSpec("10.4.108.214", 5432), 1);
//        Properties info = new Properties();
//        QueryExecutorImpl a = new QueryExecutorImpl()
//        Query x = a.createSimpleQuery(sql);
        List<SQLStatement> a = SQLUtils.parseStatements ( sql, JdbcConstants.MYSQL );

        if (AiShuUtil.isNotEmpty(a)) {
            for (SQLStatement s : a) {
                //Create语句处理
                if (s instanceof SQLCreateTableStatement) {
                    SQLCreateTableStatement  s1 = (SQLCreateTableStatement) s;
                    SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                    String schema =  sqlExprTableSource.getSchema();
                    String catalog =  sqlExprTableSource.getCatalog();
                    String table = sqlExprTableSource.getTableName();
                    if (AiShuUtil.isNotEmpty(s1.getComment())) {
                        SQLCharExpr commentExpr = (SQLCharExpr) s1.getComment();
                        String comment = commentExpr.getText();
                    }
                    table = null;
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
                            SQLCharExpr charExpr = (SQLCharExpr) s1.getComment();
                            String comment = charExpr.getText();
                        } else {
                            SQLIdentifierExpr tableExpr = (SQLIdentifierExpr) columnExpr.getOwner();
                            table = tableExpr.getName();
                        }
                        table = null;
                    } else {
                        String table = exprTableSource.getTableName();
                        String schema = exprTableSource.getSchema();
                        String catalog = exprTableSource.getCatalog();
                        LiveDdlDto liveDdlDto = new LiveDdlDto();
                        liveDdlDto.setOriginCatalog(catalog);
                        SQLCharExpr charExpr = (SQLCharExpr) s1.getComment();
                        String comment = charExpr.getText();
                        table = null;
                    }
                }

                if (s instanceof SQLCommentStatement) {
                    SQLCommentStatement  s1 = (SQLCommentStatement) s;
                    SQLExprTableSource exprTableSource = s1.getOn();
                    String column = exprTableSource.getName().getSimpleName();
                    String table = exprTableSource.getTableName();
                    String schema =  exprTableSource.getSchema();
                    String catalog =  exprTableSource.getCatalog();
                    SQLCommentStatement.Type type = s1.getType();
                    String typeName =  type.name();
                    SQLCharExpr charExpr = (SQLCharExpr) s1.getComment();
                    String comment = charExpr.getText();
                    comment = charExpr.getText();
                }

                if (s instanceof SQLAlterTableStatement) {
                    SQLAlterTableStatement  s1 = (SQLAlterTableStatement) s;
                    SQLExprTableSource sqlExprTableSource = s1.getTableSource();
                    String column = sqlExprTableSource.getName().getSimpleName();
                    String table = sqlExprTableSource.getTableName();
                    String schema =  sqlExprTableSource.getSchema();
                    String catalog =  sqlExprTableSource.getCatalog();
                    List<SQLAssignItem> assignItemList = s1.getTableOptions();
                    if (AiShuUtil.isNotEmpty(assignItemList)) {
                        for (SQLAssignItem item : assignItemList) {
                            if (item.getTarget() instanceof SQLIdentifierExpr) {
                                SQLIdentifierExpr target = (SQLIdentifierExpr) item.getTarget();
                                if ("COMMENT".equals(target.getName())) {
                                    SQLCharExpr sqlCharExpr = (SQLCharExpr) item.getValue();
                                    String comment = sqlCharExpr.getText();
                                }
                            }
                        }
                    }
                    if (AiShuUtil.isNotEmpty(s1.getItems())) {
                        //说明是字段级改动
                    }
                    catalog = null;
                }

                if (s instanceof SQLDropTableStatement) {
                    SQLDropTableStatement  s1 = (SQLDropTableStatement) s;
                    List<SQLExprTableSource> sqlExprTableSources = s1.getTableSources();
                    for (SQLExprTableSource sqlExprTableSource : sqlExprTableSources) {
                        String column = sqlExprTableSource.getName().getSimpleName();
                        String table = sqlExprTableSource.getTableName();
                        String schema = sqlExprTableSource.getSchema();
                        String catalog = sqlExprTableSource.getCatalog();
                        catalog = null;
                    }
                }

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
                            }
                            if (item.getName() instanceof SQLPropertyExpr) {
                                SQLPropertyExpr oldExpr = (SQLPropertyExpr) item.getName();
                                String oldName = oldExpr.getName();
                                SQLIdentifierExpr oldOwner = (SQLIdentifierExpr) oldExpr.getOwner();
                                String schema = oldOwner.getName();
                                SQLPropertyExpr newExpr = (SQLPropertyExpr) item.getTo();
                                String newName = newExpr.getName();
                            }

                        }
                    }

                }
            }
        }
        return Result.success(sql);
    }

    @ApiOperation(value = "03.binlog解析测试", notes = "binlog解析测试")
    @PostMapping(value = "/binlogTest")
    public Result<?> binlog(
            @Validated @RequestBody String idStr
    ) {
        Long dsid = ConvertUtil.toLong(idStr);
            EventDeserializer eventDeserializer = new EventDeserializer();
            eventDeserializer.setCompatibilityMode(
                    EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                    EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
            );

            for (EventType eventType : EventType.values()) {
                // 对每个 eventType 进行操作
                if (!eventType.equals(EventType.QUERY) && !eventType.equals(EventType.ANONYMOUS_GTID)) {
                    eventDeserializer.setEventDataDeserializer(eventType, new NullEventDataDeserializer());
                }
            }
            BinaryLogClient client = new BinaryLogClient("10.4.71.29", 3306, "af_virtualization_01", "root", "Eisoo.com123");
            binaryLogClientMap.put(dsid, client);
            client.setEventDeserializer(eventDeserializer);
            client.registerEventListener(new BinaryLogClient.EventListener() {
                @Override
                public void onEvent(Event event) {
                    EventHeader eventHeader = event.getHeader();
                    EventType eventType = eventHeader.getEventType();
                    log.info("全binlog事件打印,eventType:{},eventData:{}", eventType.name(),event.getData());
                    if (eventType.equals(EventType.QUERY)) {
                        log.info("eventType.name:{}", eventType.name());
                        log.info("className:{},classContent:{}", event.getData().getClass().getName(), event.getData());

                        QueryEventData queryEventData = event.getData();
                        log.info("queryEventData.getExecutionTime:{}", queryEventData.getExecutionTime());
                        if (!"BEGIN".equals(queryEventData.getSql())) {
                            LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                            taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.START_BINLOG.getCode());
                            taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING);
                            if (taskService.count(taskEntityLambdaQueryWrapper) > 0l) {
                                log.info("queryEventData.getDatabase:{},queryEventData.sql:{}", queryEventData.getDatabase(), queryEventData.getSql());
                            } else {
                                try {
                                    binaryLogClientMap.get(dsid).disconnect();
                                } catch (Exception e) {
                                    log.error(e.toString());
                                }

                            }
                        }
                    }

                    if (eventType.equals(EventType.ANONYMOUS_GTID)) {
                        log.info("eventType.name:{}", eventType.name());
                        log.info("className:{},classContent:{}", event.getData().getClass(), event.getData());
//                        log.info("queryEventData.getExecutionTime:{}", queryEventData.getExecutionTime());
//                        if (!"BEGIN".equals(queryEventData.getSql())) {
//                            log.info("queryEventData.getDatabase:{},queryEventData.sql:{}", queryEventData.getDatabase(), queryEventData.getSql());
//                        }
                    }


                }
            });
            Runnable listen = () -> {
                try {
                    client.connect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            Thread t1 = new Thread(listen);
            t1.start();

        return Result.success(dsid);
    }
    @ApiOperation(value = "04.binlog解析中止测试", notes = "binlog解析测试")
    @PostMapping(value = "/stopbinlogTest")
    public Result<?> stopbinlog(
            @Validated @RequestBody String idstr
    ) throws IOException {
        Long dsid = ConvertUtil.toLong(idstr);
        binaryLogClientMap.get(dsid).disconnect();
        return Result.success(dsid);
    }
}
