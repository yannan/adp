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
 * 表字段关系映射表
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Setter
@Getter
@TableName("t_lineage_edge_column_table_relation")
public class LineageEdgeColumnTableRelationEntity extends LineageParentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 表ID
     */
    @TableField(value = "f_table_id")
    private String tableId;

    /**
     * 字段ID
     */
    @TableField(value = "f_column_id")
    private String columnId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineageEdgeColumnTableRelationEntity)) return false;
        LineageEdgeColumnTableRelationEntity entity = (LineageEdgeColumnTableRelationEntity) o;
        return tableId.equals(entity.tableId) && columnId.equals(entity.columnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableId, columnId);
    }

    @Override
    public String toString() {
        return "LineageEdgeColumnTableRelationEntity[" +
                "tableId:" + (tableId == null ? "" : tableId) +
                ",columnId:" + (columnId == null ? "" : columnId) +
                "]";
    }
}
