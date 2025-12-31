package com.eisoo.lineage.presto;

import com.eisoo.lineage.CommonUtil;
import io.prestosql.sql.tree.*;

import java.util.List;

public class HandlerExpressionUtil {
    private static void visitSQLIdentifierExpr(Identifier columnExpr, TreeNode<LineageDolphinColumn> targetNode) {
        LineageDolphinColumn targetNodeData = targetNode.getData();
        LineageDolphinColumn project = new LineageDolphinColumn();
        project.setTargetColumnName(columnExpr.getValue());

        if (CommonUtil.isNotEmpty(targetNodeData.getDereferenceTabAlias())) {
            project.setDereferenceTabAlias(targetNodeData.getDereferenceTabAlias());
        }
        if (CommonUtil.isNotEmpty(targetNodeData.getExpression())) {
            project.setExpression(targetNodeData.getExpression());
        }

        TreeNode<LineageDolphinColumn> search = targetNode.findChildNode(project);
        if (CommonUtil.isEmpty(search)) {
            targetNode.addChild(project);
        }
    }

    private static void visitSQLMethodInvoke(FunctionCall expr, TreeNode<LineageDolphinColumn> node) {
        if (expr.getArguments().isEmpty()) {
            //计算表达式，没有更多列，结束循环
            if (node.getData().getExpression().equals(expr.toString())) {
                node.getData().setIsEnd(true);
            }
        } else {
            List<Expression> arguments = expr.getArguments();
            //TODO:concat(test,user_name) as user_name:处理参数与node的关系！test->user_name
            for (Expression argument : arguments) {
                handlerExpr(argument, node);
            }
        }
    }

    /***
     * TODO:处理表达式与字段的关系：需要扩展,覆盖掉所有的表达式！
     */
    public static void handlerExpr(Expression columnExpr, TreeNode<LineageDolphinColumn> targetNode) {
        //TODO:生成函数表达式的处理
        if (columnExpr instanceof FunctionCall) {
            visitSQLMethodInvoke((FunctionCall) columnExpr, targetNode);
            //TODO:处理uid 类似于这样的处理
        } else if (columnExpr instanceof Identifier) {
            visitSQLIdentifierExpr((Identifier) columnExpr, targetNode);
            //TODO:处理t1.uid 类似于这样的处理
        } else if (columnExpr instanceof DereferenceExpression) {
            visitSQLDereferenceExpr((DereferenceExpression) columnExpr, targetNode);
        } else if (columnExpr instanceof Cast) {
            Expression expression = ((Cast) columnExpr).getExpression();
            handlerExpr(expression, targetNode);
        } else if (columnExpr instanceof SearchedCaseExpression) {
            List<WhenClause> whenClauses = ((SearchedCaseExpression) columnExpr).getWhenClauses();
            for (WhenClause whenClause : whenClauses) {
                handlerExpr(whenClause.getOperand(), targetNode);
            }
        } else if (columnExpr instanceof SimpleCaseExpression) {
            Expression operand = ((SimpleCaseExpression) columnExpr).getOperand();
            handlerExpr(operand, targetNode);
        } else if (columnExpr instanceof LogicalBinaryExpression) {
            handlerExpr(((LogicalBinaryExpression) columnExpr).getLeft(), targetNode);
            handlerExpr(((LogicalBinaryExpression) columnExpr).getRight(), targetNode);
        } else if (columnExpr instanceof IsNotNullPredicate) {
            handlerExpr(((IsNotNullPredicate) columnExpr).getValue(), targetNode);
        } else if (columnExpr instanceof IsNullPredicate) {
            handlerExpr(((IsNullPredicate) columnExpr).getValue(), targetNode);
        } else if (columnExpr instanceof BetweenPredicate) {
            handlerExpr(((BetweenPredicate) columnExpr).getValue(), targetNode);
            handlerExpr(((BetweenPredicate) columnExpr).getMax(), targetNode);
            handlerExpr(((BetweenPredicate) columnExpr).getMin(), targetNode);
        } else if (columnExpr instanceof LikePredicate) {
            handlerExpr(((LikePredicate) columnExpr).getValue(), targetNode);
        } else if (columnExpr instanceof ComparisonExpression) {
            handlerExpr(((ComparisonExpression) columnExpr).getLeft(), targetNode);
            handlerExpr(((ComparisonExpression) columnExpr).getRight(), targetNode);
        } else if (columnExpr instanceof IfExpression) {
            handlerExpr(((IfExpression) columnExpr).getCondition(), targetNode);
            handlerExpr(((IfExpression) columnExpr).getTrueValue(), targetNode);
            handlerExpr(((IfExpression) columnExpr).getFalseValue().get(), targetNode);
        }
    }

    private static void visitSQLDereferenceExpr(DereferenceExpression columnExpr, TreeNode<LineageDolphinColumn> targetNode) {
        Expression tableAlias = columnExpr.getBase();//t1
        Identifier field = columnExpr.getField();//user_id
        LineageDolphinColumn project = new LineageDolphinColumn();
        project.setTargetColumnName(field.getValue());
        project.setDereferenceTabAlias(tableAlias.toString());
        if (CommonUtil.isNotEmpty(targetNode.getData().getExpression())) {
            project.setExpression(targetNode.getData().getExpression());
        }
        TreeNode<LineageDolphinColumn> search = targetNode.findChildNode(project);
        if (CommonUtil.isEmpty(search)) {
            targetNode.addChild(project);
        }
    }
}
