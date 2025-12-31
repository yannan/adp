package com.eisoo.lineage.presto;

import com.eisoo.lineage.CommonUtil;
import com.eisoo.lineage.LineageUtil;
import io.prestosql.sql.tree.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class HandlerSQLTableUtil {
    /***
     * 处理table与node的入口方法
     * @param rootNode
     * @param table
     * @throws URISyntaxException
     */
    public static void handlerTable(TreeNode<LineageDolphinColumn> rootNode, Relation table) throws URISyntaxException {
        if (table instanceof Table) { // 普通单表
            handlerSQLTableSource(rootNode, (Table) table);
        } else if (table instanceof Join) {
            handlerSQLJoinTable(rootNode, (Join) table);//处理join：t1 left jon t2
        } else if (table instanceof TableSubquery) {
            handlerSQLSubqueryTable(rootNode, (TableSubquery) table);// 处理 (select a,b from table) t:()里面的就是TableSubquery
        } else if (table instanceof AliasedRelation) {
            handlerSQLAliasedTable(rootNode, (AliasedRelation) table);// 处理 (select a,b from table) t：整体就是AliasedRelation
        }
    }

    /**
     * 处理物理表与field的血缘关系的入口方法
     */
    public static void handlerSQLTableSource(TreeNode<LineageDolphinColumn> rootNode, Table table) {
        QualifiedName name = table.getName();
        String tableName = name.getSuffix();//表名字
        Optional<QualifiedName> prefix = name.getPrefix();
        if (!prefix.isPresent()) {
            log.error("handlerLineageColumnAndTable failed ! because catalog and db not exist!");
            throw new RuntimeException();
        }
        String db = prefix.get().toString();
//        String alias = EmptyUtils.isNotEmpty ( table.getAlias () ) ? tableSource.getAlias ().replace ( "`", "" ) : "";//t1
        List<TreeNode<LineageDolphinColumn>> treeNodes = rootNode.getChildren();
        for (TreeNode<LineageDolphinColumn> treeNode : treeNodes) {
            List<TreeNode<LineageDolphinColumn>> childrens = treeNode.getChildren();
            for (TreeNode<LineageDolphinColumn> children : childrens) {
                if (CommonUtil.isNotEmpty(db)) {
                    children.getData().setSourceDbName(db);
                }
                if (children.getData().getSourceTableName() == null || children.getData().getSourceTableName().equals(tableName)) {
                    children.getData().setSourceTableName(table.getName().toString());
                    children.getData().setIsEnd(true);
                    children.getData().setExpression(treeNode.getData().getExpression());
                }
            }
        }
    }

    /**
     * 处理AliasedTable的表
     */
    public static void handlerSQLAliasedTable(TreeNode<LineageDolphinColumn> node, AliasedRelation table) throws URISyntaxException {
        Relation relation = table.getRelation();
        String tableAliasName = table.getAlias().getValue();
        // 第一种情况：from (select a,b from table) t,from后面的就是TableSubquery
        if (relation instanceof TableSubquery) {
            TableSubquery tableSubquery = (TableSubquery) relation;
            QuerySpecification queryBodyRelation = (QuerySpecification) tableSubquery.getQuery().getQueryBody();
            Set<TreeNode<LineageDolphinColumn>> allLeafs = node.getAllLeafs();
            for (TreeNode<LineageDolphinColumn> leaf : allLeafs) {
                Boolean isEnd = leaf.getData().getIsEnd();
                if (!isEnd) {
                    leaf.getData().setDereferenceTabAlias(tableAliasName);
                    LineageUtil.buildAnalyzerLineage(queryBodyRelation, leaf);
                }
            }
            // 第二种情况：from  tableA t,from后面的就是Table
        } else if (relation instanceof Table) {
            Set<TreeNode<LineageDolphinColumn>> allLeafs = node.getAllLeafs();
            for (TreeNode leaf : allLeafs) {
                LineageDolphinColumn data = (LineageDolphinColumn) leaf.getData();
                Boolean isEnd = data.getIsEnd();
                String dereferenceTabAlias = data.getDereferenceTabAlias();
                if (!isEnd && CommonUtil.isNotEmpty(dereferenceTabAlias) && tableAliasName.equals(dereferenceTabAlias)) {
                    handlerLineageColumnAndTable(leaf, (Table) relation, false);
                } else if (!isEnd && CommonUtil.isEmpty(dereferenceTabAlias)) {
                    // 处理这种需要判断table里面是不是有这个字段，因为t1.age => age,没有了别名
                    handlerLineageColumnAndTable(leaf, (Table) relation, true);
                }
            }
        }
    }

    /**
     * 处理物理表与field的血缘关系
     */
    private static void handlerLineageColumnAndTable(TreeNode<LineageDolphinColumn> leaf, Table table, boolean isNeedJudgeMeaContainsThisColumn) {
        //TODO:需要从table的元数据信息判断是否有这个字段
//        if (isNeedJudgeMeaContainsThisColumn) {
//        }
        QualifiedName name = table.getName();
        //1,获取table的名字：mysql.d1.user
        String tableName = name.getSuffix();
        //2,获取catalog和db的名字：user
        Optional<QualifiedName> prefix = name.getPrefix();
        if (!prefix.isPresent()) {
            log.error("handlerLineageColumnAndTable failed ! because catalog and db not exist!");
            throw new RuntimeException();
        }
        String db = prefix.get().toString();//mysql.d1
        leaf.getData().setSourceDbName(db);
        leaf.getData().setSourceTableName(table.getName().toString());
        leaf.getData().setExpression(leaf.getData().getExpression());
        leaf.getData().setSourceColumnName(leaf.getData().getTargetColumnName());
        leaf.getData().setIsEnd(true);
    }

    private static void handlerSQLSubqueryTable(TreeNode<LineageDolphinColumn> node, TableSubquery table) throws URISyntaxException {
        QueryBody queryBody = table.getQuery().getQueryBody();
        Set<TreeNode<LineageDolphinColumn>> allLeafs = node.getAllLeafs();
        for (TreeNode<LineageDolphinColumn> leaf : allLeafs) {
            if (!leaf.getData().getIsEnd()) {
                LineageUtil.buildAnalyzerLineage(queryBody, leaf);
            }
        }
    }

    private static void handlerSQLUnionTable(TreeNode<LineageDolphinColumn> rootNode, Union table) throws URISyntaxException {
        List<Relation> relations = table.getRelations();
        for (Relation relation : relations) {
            if (relation instanceof Table) { // 普通单表
                handlerSQLTableSource(rootNode, (Table) relation);
            } else if (relation instanceof Join) {
                handlerSQLJoinTable(rootNode, (Join) relation);//处理join
            } else if (relation instanceof TableSubquery) {
                handlerSQLSubqueryTable(rootNode, (TableSubquery) relation);// 处理 subquery
            } else if (relation instanceof AliasedRelation) {
                handlerSQLAliasedTable(rootNode, (AliasedRelation) relation);// 处理 AliasedRelation
            }
        }
    }

    private static void handlerSQLJoinTable(TreeNode<LineageDolphinColumn> node, Join table) throws URISyntaxException {//两个表的join
        Set<TreeNode<LineageDolphinColumn>> allLeafs = node.getAllLeafs();
        for (TreeNode<LineageDolphinColumn> child : allLeafs) {
            Boolean isEnd = child.getData().getIsEnd();
            if (!isEnd) {
                Relation left = table.getLeft();
                if (left instanceof Join) {
                    handlerSQLJoinTable(node, (Join) left);
                } else if (left instanceof Table) {
                    handlerSQLTableSource(node, (Table) left);
                } else if (left instanceof TableSubquery) {
                    handlerSQLSubqueryTable(node, (TableSubquery) left);
                } else if (left instanceof Union) {
                    handlerSQLUnionTable(node, (Union) left);
                } else if (left instanceof AliasedRelation) {
                    handlerSQLAliasedTable(node, (AliasedRelation) left);
                }
            }
        }
        for (TreeNode<LineageDolphinColumn> child : allLeafs) {
            Boolean isEnd = child.getData().getIsEnd();
            if (!isEnd) {
                Relation right = table.getRight();
                if (right instanceof Join) {
                    handlerSQLJoinTable(node, (Join) right);
                } else if (right instanceof Table) {
                    handlerSQLTableSource(node, (Table) right);
                } else if (right instanceof TableSubquery) {
                    handlerSQLSubqueryTable(node, (TableSubquery) right);
                } else if (right instanceof Union) {
                    handlerSQLUnionTable(node, (Union) right);
                } else if (right instanceof AliasedRelation) {
                    handlerSQLAliasedTable(node, (AliasedRelation) right);
                }
            }
        }
    }
}
