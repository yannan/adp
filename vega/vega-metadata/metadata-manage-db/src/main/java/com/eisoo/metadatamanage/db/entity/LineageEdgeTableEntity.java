package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 表血缘关系
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Setter
@Getter
@TableName("t_lineage_edge_table")
public class LineageEdgeTableEntity extends LineageParentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 源字段ID
     */
    @TableField(value = "f_parent_id")
    private String parentId;

    /**
     * 目标字段ID
     */
    @TableField(value = "f_child_id")
    private String childId;

    /**
     * 创建类型： HIVE/DATAX/SPARK/USER_REPORT
     */
    @TableField(value = "f_create_type")
    private String createType;

    /**
     * 创建时间，时间戳
     */
    @TableField(value = "f_create_time")
    private Date createTime;


    /**
     *
     */
    @TableField(value = "f_query_text")
    private String queryText;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineageEdgeTableEntity)) return false;
        LineageEdgeTableEntity entity = (LineageEdgeTableEntity) o;
        return parentId.equals(entity.parentId) && childId.equals(entity.childId) && createType.equals(entity.createType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, childId, createType);
    }

    @Override
    public String toString() {
        return "LineageEdgeTableEntity[" +
                "createType:" + (createType == null ? "" : createType) +
                ",parentId:" + (parentId == null ? "" : parentId) +
                ",childId:" + (childId == null ? "" : childId) +
                "]";
    }


}
