package com.eisoo.lineage;

import com.eisoo.lineage.presto.HandlerSQLTableUtil;
import com.eisoo.lineage.presto.HandlerSelectItemUtil;
import com.eisoo.lineage.presto.LineageDolphinColumn;
import com.eisoo.lineage.presto.TreeNode;
import io.prestosql.sql.parser.ParsingOptions;
import io.prestosql.sql.parser.SqlParser;
import io.prestosql.sql.tree.*;
import lombok.extern.slf4j.Slf4j;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
public class LineageUtil {
    /**
     * 解析sql血缘的入口方法
     */
    public static HashMap<String, ArrayList<LineageDolphinColumn>> getLineageBySql(String sql) throws URISyntaxException {
        TreeNode<LineageDolphinColumn> rootNode = new TreeNode<>(new LineageDolphinColumn());
        log.warn("开始解析sql血缘：\n{}",sql);
        changeSqlTextToQuery(sql, rootNode);
        HashMap<String, ArrayList<LineageDolphinColumn>> map = new HashMap<>();
        for (TreeNode<LineageDolphinColumn> sourceColumnNode : rootNode.getChildren()) {
            Set<LineageDolphinColumn> leafNodes = sourceColumnNode.getAllLeafData();
            for (LineageDolphinColumn lineageDolphinColumn : leafNodes) {
                if (lineageDolphinColumn.getIsEnd()) {
                    String targetColumnName = sourceColumnNode.getData().getTargetColumnName();
                    ArrayList<LineageDolphinColumn> list = map.get(targetColumnName);
                    if (null == list) {
                        list = new ArrayList<LineageDolphinColumn>();
                    }
                    list.add(lineageDolphinColumn);
                    map.put(targetColumnName, list);
                }
            }
        }
        return map;
    }

    /***
     * 从sqlText中截取Query从而解析血缘
     */
    public static void changeSqlTextToQuery(String sqlText, TreeNode<LineageDolphinColumn> rootNode) throws URISyntaxException {
        if (CommonUtil.isEmpty(sqlText)) {
            return;
        }
        Statement statement = new SqlParser().createStatement(sqlText, new ParsingOptions(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL));
        if (statement instanceof Query) {
            Query query = (Query) statement;
            QueryBody queryBody = query.getQueryBody();
            buildAnalyzerLineage(queryBody, rootNode);
        } else if (statement instanceof Insert) {
            Insert insert = (Insert) statement;
            String suffix = insert.getTarget().toString();
            Query query = insert.getQuery();
            QueryBody queryBody = query.getQueryBody();
            buildAnalyzerLineage(queryBody, rootNode);
        }
    }

    public static void buildAnalyzerLineage(QueryBody queryBody, TreeNode<LineageDolphinColumn> rootNode) throws URISyntaxException {
        AtomicReference<Boolean> isContinue = new AtomicReference<>(false);
        if (queryBody instanceof QuerySpecification) {
            QuerySpecification querySpecification = (QuerySpecification) queryBody;
            buildQuerySpecificationLineage(querySpecification, rootNode, isContinue);
        }
    }

    /**
     * 从QuerySpecification解析血缘，挂载到rootNode上
     */
    public static void buildQuerySpecificationLineage(QuerySpecification specification, TreeNode<LineageDolphinColumn> rootNode, AtomicReference<Boolean> isContinue) throws URISyntaxException {
        List<SelectItem> selectItems = specification.getSelect().getSelectItems();
        for (SelectItem item : selectItems) {
            // 处理select * 的情况
            if (item instanceof AllColumns) {
                AllColumns allColumns = (AllColumns) item;
                Optional<QualifiedName> prefix = allColumns.getPrefix();
                String alias = "";
                if (prefix.isPresent()) {
                    alias = prefix.get().toString();
                }
                Optional<Relation> fromRelation = specification.getFrom();
                if (fromRelation.isPresent()) {
                    List<String> itemFromRelation = HandlerSelectItemUtil.getItemFromRelation(fromRelation.get(), alias);
                    for (String column : itemFromRelation) {
                        Expression expression;
                        if (CommonUtil.isNotEmpty(alias)) {
                            expression = new DereferenceExpression(new Identifier(alias), new Identifier(column));
                        } else {
                            expression = new Identifier(column);
                        }
                        SingleColumn singleColumn = new SingleColumn(expression); // Identifier
                        HandlerSelectItemUtil.extractSingleColumnUtil(singleColumn, rootNode, isContinue);
                    }
                }
            } else if (item instanceof SingleColumn) {
                HandlerSelectItemUtil.extractSingleColumnUtil((SingleColumn) item, rootNode, isContinue);
            }
        }
        if (isContinue.get()) {
            Optional<Relation> fromRelation = specification.getFrom();
            fromRelation.ifPresent(relation -> {
                try {
                    // 处理from后面的数据
                    HandlerSQLTableUtil.handlerTable(rootNode, relation);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
