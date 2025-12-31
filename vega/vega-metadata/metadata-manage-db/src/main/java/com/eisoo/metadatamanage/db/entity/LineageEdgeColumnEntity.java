package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段血缘关系表
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Setter
@Getter
@TableName("t_lineage_edge_column")
public class LineageEdgeColumnEntity extends LineageParentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private String id;

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
     * 目标字段ID
     */
    @TableField(value = "f_child_id")
    private String childId;

    /**
     * 源字段ID
     */
    @TableField(value = "f_parent_id")
    private String parentId;

    /**
     *
     */
    @TableField(value = "f_query_text")
    private String queryText;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineageEdgeColumnEntity)) return false;
        LineageEdgeColumnEntity that = (LineageEdgeColumnEntity) o;
        return createType.equals(that.createType) && childId.equals(that.childId) && parentId.equals(that.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createType, childId, parentId);
    }

    @Override
    public String toString() {
        return "LineageEdgeColumnEntity[" +
                "createType:" + (createType == null ? "" : createType) +
                ",parentId:" + (parentId == null ? "" : parentId) +
                ",childId:" + (childId == null ? "" : childId) +
                "]";
    }
}
