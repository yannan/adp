package com.eisoo.lineage.presto;


import com.eisoo.lineage.CommonUtil;
import lombok.Data;
/**
 * @Author: Lan Tian
 * @Date: 2024/5/8 13:27
 * @Version:1.0
 */
@Data
public class LineageDolphinColumn implements Comparable<LineageDolphinColumn> {
    private String targetColumnName;
    private String sourceDbName;
    private String sourceTableName;
    private String sourceColumnName;
    private String expression;
    private String DereferenceTabAlias;
    private Boolean isEnd = false;


    public void setSourceTableName(String sourceTableName) {
        int length = sourceTableName.length();
        int indexOfSplit = 0;
        for (int index = length - 1; index >= 0; index--) {
            char[] charArray = sourceTableName.toCharArray();
            String c = String.valueOf(charArray[index]);
            if (c.equals(".")) {
                indexOfSplit = index;
                break;
            }
        }
        sourceTableName = CommonUtil.isNotEmpty(sourceTableName) ? sourceTableName.replace("`", "") : sourceTableName;
        if (sourceTableName.contains(".")) {
            this.sourceTableName = sourceTableName.substring(indexOfSplit + 1);
        } else {
            this.sourceTableName = sourceTableName;
        }
    }

    public int compareTo(LineageDolphinColumn o) {
        if (this.getTargetColumnName().equals(o.getTargetColumnName())) return 0;
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LineageDolphinColumn myColumn = (LineageDolphinColumn) o;

        if (!this.getTargetColumnName().equals(myColumn.getTargetColumnName())) return false;
        if (CommonUtil.isNotEmpty(sourceTableName) && !sourceTableName.equals(myColumn.sourceTableName)) return false;
        if (CommonUtil.isNotEmpty(sourceColumnName)) return sourceColumnName.equals(myColumn.sourceColumnName);
        return true;
    }

    @Override
    public int hashCode() {
        int result = getTargetColumnName().hashCode();
        if (CommonUtil.isNotEmpty(sourceTableName)) {
            result = 31 * result + sourceTableName.hashCode();
        }
        if (CommonUtil.isNotEmpty(sourceColumnName)) {
            result = 31 * result + sourceColumnName.hashCode();
        }
        return result;
    }
}
