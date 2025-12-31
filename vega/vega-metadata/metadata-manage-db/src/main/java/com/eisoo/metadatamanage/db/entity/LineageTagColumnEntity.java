package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Objects;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段信息
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Getter
@Setter
@TableName("t_lineage_tag_column2")
public class LineageTagColumnEntity extends LineageParentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，根据表信息和字段名称MD5计算得到
     */
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private String id;


    @TableField(value = "f_table_id")
    private String tableId;

    /**
     * 字段名称
     */
    @TableField(value = "f_column")
    private String columnName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineageTagColumnEntity)) return false;
        LineageTagColumnEntity column = (LineageTagColumnEntity) o;
        return tableId.equals(column.tableId) && columnName.equals(column.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableId, columnName);
    }

    @Override
    public String toString() {
        return "LineageTagColumnEntity[" +
                "tableId:" + tableId +
                ",column:" + (columnName == null ? "" : columnName) +
                "]";
    }
}
