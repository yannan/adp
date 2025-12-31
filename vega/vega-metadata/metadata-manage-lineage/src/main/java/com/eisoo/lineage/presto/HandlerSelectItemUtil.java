package com.eisoo.lineage.presto;

import com.alibaba.fastjson.JSONObject;
import com.eisoo.config.EngineConfiguration;
import com.eisoo.config.SpringUtil;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.metadatamanage.util.HttpUtil;
import io.prestosql.sql.parser.ParsingOptions;
import io.prestosql.sql.parser.SqlParser;
import io.prestosql.sql.tree.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/14 13:13
 * @Version:1.0
 */
@Slf4j
public class HandlerSelectItemUtil {
    public static List<String> getItemFromRelation(Relation fromRelation, String alias) throws URISyntaxException {
        List<String> selectItems = new ArrayList<>();
        if (fromRelation instanceof Table) { // 普通单表
            selectItems = handlerSQLTableSource((Table) fromRelation, alias);
        } else if (fromRelation instanceof Join) {
            selectItems = handlerSQLJoinTable((Join) fromRelation, alias);
        } else if (fromRelation instanceof TableSubquery) {
            selectItems = handlerSQLSubqueryTable((TableSubquery) fromRelation, alias);
        } else if (fromRelation instanceof AliasedRelation) {
            selectItems = handlerSQLAliasedTable((AliasedRelation) fromRelation, alias);
        }
        return selectItems;
    }

    private static List<String> handlerSQLJoinTable(Join table, String alias) throws URISyntaxException {
        // 如果是空，left + right
        Relation left = table.getLeft();
        List<String> selectItemsLeft = new ArrayList<>();
        List<String> selectItemsRight = new ArrayList<>();
        if (left instanceof Join) {
            selectItemsLeft = handlerSQLJoinTable((Join) left, alias);
        } else if (left instanceof Table) {
            selectItemsLeft = handlerSQLTableSource((Table) left, alias);
        } else if (left instanceof TableSubquery) {
            selectItemsLeft = handlerSQLSubqueryTable((TableSubquery) left, alias);
        } else if (left instanceof AliasedRelation) {
            selectItemsLeft = handlerSQLAliasedTable((AliasedRelation) left, alias);
        }
        Relation right = table.getRight();
        if (right instanceof Join) {
            selectItemsRight = handlerSQLJoinTable((Join) right, alias);
        } else if (left instanceof Table) {
            selectItemsRight = handlerSQLTableSource((Table) right, alias);
        } else if (left instanceof TableSubquery) {
            selectItemsRight = handlerSQLSubqueryTable((TableSubquery) right, alias);
        } else if (left instanceof AliasedRelation) {
            selectItemsRight = handlerSQLAliasedTable((AliasedRelation) right, alias);
        }
        selectItemsLeft.addAll(selectItemsRight);
        return selectItemsLeft;
    }


    /**
     * 从虚拟引擎元数据服务获取table的字段name的列表
     *
     * @param catalog
     * @param schema
     * @param table
     * @return
     */
    private static List<String> getColumnNameFromVirtualEngineService(String catalog, String schema, String table) throws Exception {
        String virtualizationUri = "%s://%s:%s/api/virtual_engine_service%s";
        EngineConfiguration engineConfiguration = SpringUtil.getBean(EngineConfiguration.class);
        String url = String.format(virtualizationUri,
                                   engineConfiguration.getProtocol(),
                                   engineConfiguration.getHost(),
                                   engineConfiguration.getPort(),
                                   engineConfiguration.getColumnApi());
        url = String.format(url, catalog, schema, table);
        HashMap<String, String> head = new HashMap<>();
        head.put("X-Presto-User", engineConfiguration.getUser());
        String response;
        List<String> selectItems = new ArrayList<>();
        try {
            response = HttpUtil.executeGet(url, head);
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (jsonObject.containsKey("data")) {
                List<ColumnMetaEntity> data = jsonObject.getJSONArray("data").toJavaList(ColumnMetaEntity.class);
                for (ColumnMetaEntity column : data) {
                    selectItems.add(column.getName());
                }
            } else {
                log.error("getColumnNameFromVirtualEngineService failed!url={},details is follow:{}", url, response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("getColumnNameFromVirtualEngineService failed!url={},details is follow:{}", url, e.getMessage());
            throw new Exception(e);
        }
        return selectItems;
    }

    private static List<String> handlerSQLTableSource(Table table, String alias) {
        QualifiedName name = table.getName();
        String tableName = name.getSuffix();//表名字
        Optional<QualifiedName> prefix = name.getPrefix();//cat.db
        if (!prefix.isPresent()) {
            log.error("=====================tableName:{} not has prefix!=======================", prefix);
            throw new RuntimeException();
        }
        String db = prefix.get().toString();//cat.db
        String[] catAndDb = db.split("\\.");
        //根据table信息查出来元数据信息返回
        List<String> columnNameFromVirtualEngineService = null;
        try {
            columnNameFromVirtualEngineService = getColumnNameFromVirtualEngineService(catAndDb[0], catAndDb[1], tableName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return columnNameFromVirtualEngineService;
    }

    private static List<String> handlerSQLSubqueryTable(TableSubquery table, String alias) throws URISyntaxException {
        List<String> selectItemsLeft = new ArrayList<>();//(select * from   postgresql_c0df05de19cc4c408e8e4f36d065833f.pure.dwd_user_dim_d) a
        Query query = table.getQuery();
        return getSelectItemListFromQuery(query);
    }

    private static List<String> handlerSQLAliasedTable(AliasedRelation table, String alias) throws URISyntaxException {
        Relation relation = table.getRelation();
        String tableAliasName = table.getAlias().getValue();
        List<String> selectItemsLeft = new ArrayList<>();
        if (alias.equals(tableAliasName)) {
            if (relation instanceof TableSubquery) {
                TableSubquery tableSubquery = (TableSubquery) relation;
                selectItemsLeft = handlerSQLSubqueryTable(tableSubquery, alias);
            } else if (relation instanceof Table) {
                selectItemsLeft = handlerSQLTableSource((Table) relation, alias);
            }
        }
        return selectItemsLeft;
    }

    /***
     * 从Query获取select 后面的field列表，支持*的形式:
     */
    public static List<String> getSelectItemListFromQuery(Query query) throws URISyntaxException {
        QuerySpecification queryBody = (QuerySpecification) query.getQueryBody();
        List<SelectItem> selectItems = queryBody.getSelect().getSelectItems();
        ArrayList<String> selectItemList = new ArrayList<>();
        for (SelectItem item : selectItems) {
            if (item instanceof AllColumns) {
                AllColumns allColumns = (AllColumns) item;
                Optional<QualifiedName> prefix = allColumns.getPrefix();
                String alias = "";
                if (prefix.isPresent()) {
                    alias = prefix.get().toString();
                }
                Optional<Relation> fromRelation = queryBody.getFrom();
                if (fromRelation.isPresent()) {
                    List<String> itemFromRelation = HandlerSelectItemUtil.getItemFromRelation(fromRelation.get(), alias);
                    selectItemList.addAll(itemFromRelation);
                }
            } else if (item instanceof SingleColumn) {
                SingleColumn selectItemColumn = (SingleColumn) item;
                String columnCurrent = selectItemColumn.getAlias().isPresent() ? selectItemColumn.getAlias().get().toString() : selectItemColumn.getExpression().toString();
                if (columnCurrent.contains(".")) {
                    columnCurrent = columnCurrent.substring(columnCurrent.indexOf(".") + 1);
                }
                columnCurrent = columnCurrent.replace("`", "");//uid
                selectItemList.add(columnCurrent);
            }
        }
        return selectItemList;
    }

    /***
     * 从Create table 获取field列表
     */
    public static HashMap<String, List<String>> getFieldListFromCreteSQL(String sqlText) {
        HashMap<String, List<String>> result = new HashMap<>();
        List<String> list = new ArrayList<>();
        if (CommonUtil.isEmpty(sqlText)) {
            return result;
        }
        Statement statement = new SqlParser().createStatement(sqlText, new ParsingOptions(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL));
        if (statement instanceof CreateTable) {
            CreateTable query = (CreateTable) statement;
            List<Identifier> originalParts = query.getName().getOriginalParts();
            List<String> tableInfo = originalParts.stream().map(t -> t.getValue()).collect(Collectors.toList());
            result.put("table", tableInfo);
            List<TableElement> elements = query.getElements();
            for (TableElement t : elements) {
                JdbcColumnDefinition tmp = (JdbcColumnDefinition) t;
                list.add(tmp.getName().getValue());
            }
        }
        result.put("column", list);
        return result;
    }

    /***
     * 从sql文本获取select 后面的field列表，支持*的形式:
     */
    public static List<String> getSelectItemListFromSQL(String sqlInsertText) throws URISyntaxException {
        List<String> selectItemListFromQuery = new ArrayList<>();
        Statement statement = null;
        try {
            statement = new SqlParser().createStatement(sqlInsertText, new ParsingOptions(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL));
        } catch (Exception e) {
            log.error("=================Presto SqlParser Failed!Sql={}==================", sqlInsertText);
            throw new RuntimeException(e);
        }
        //TODO:从不同形式的statement中截取query,这里根据需要补充不同的statement，例如create table等等
        if (statement instanceof Query) {
            Query query = (Query) statement;
            selectItemListFromQuery = getSelectItemListFromQuery(query);
        } else if (statement instanceof Insert) {
            Insert insert = (Insert) statement;
            String suffix = insert.getTarget().toString();//mysql_letb2t2n.dmw.view_creater
            Query query = insert.getQuery();
            selectItemListFromQuery = getSelectItemListFromQuery(query);
        }
        return selectItemListFromQuery;
    }

    public static void extractSingleColumnUtil(SingleColumn selectItemColumn, TreeNode<LineageDolphinColumn> rootNode, AtomicReference<Boolean> isContinue) {
        String columnCurrent = selectItemColumn.getAlias().isPresent() ? selectItemColumn.getAlias().get().toString() : selectItemColumn.getExpression().toString();
        if (columnCurrent.contains(".")) {
            columnCurrent = columnCurrent.substring(columnCurrent.indexOf(".") + 1);
        }
        columnCurrent = columnCurrent.replace("`", "");//uid
        String expr = selectItemColumn.getExpression().toString();//user_id
        //1,处理目标列：user_id
        LineageDolphinColumn targetColumn = new LineageDolphinColumn();
        targetColumn.setTargetColumnName(columnCurrent);//uid
        //2,处理目标列与上层的关系
        LineageDolphinColumn parentData = rootNode.getData();
        String parentExpression = parentData.getExpression();
        String parentDereferenceTabAlias = parentData.getDereferenceTabAlias();
        if (CommonUtil.isNotEmpty(parentExpression)) {
            expr = parentExpression + "<-" + expr;
        }
        if (CommonUtil.isNotEmpty(parentDereferenceTabAlias)) {
            targetColumn.setDereferenceTabAlias(parentDereferenceTabAlias);
        }
        targetColumn.setExpression(expr);// user_id
        TreeNode<LineageDolphinColumn> targetNode = new TreeNode<>(targetColumn);//uid
        //处理concat(t1.user_id,t2.user_name) as baozi：baozi 与 concat(t1.user_id,t2.user_name) 关系
        Expression columnExpr = selectItemColumn.getExpression();  //处理表达式:name='user_id'
        HandlerExpressionUtil.handlerExpr(columnExpr, targetNode);//SQLIdentifierExpr
        if (rootNode.getLevel() == 0 || rootNode.getData().getTargetColumnName().equals(columnCurrent)) {
            rootNode.addChild(targetNode);
            isContinue.set(true);
        }
    }
}
