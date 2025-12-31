package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Objects;

import com.eisoo.standardization.common.util.AiShuUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 表信息
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Getter
@Setter
@TableName("t_lineage_tag_table2")
public class LineageTagTableEntity extends LineageParentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，根据表信息MD5计算得来
     */
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 数据库类型
     */
    @TableField(value = "f_db_type")
    private String dbType;


    /**
     * 数据元ID
     */
    @TableField(value = "f_ds_id")
    private String dsId;

    /**
     * 数据库连接URL
     */
    @TableField(value = "f_jdbc_url")
    private String jdbcUrl;

    /**
     * 数据库JDBC 用户名
     */
    @TableField(value = "f_jdbc_user")
    private String jdbcUser;

    /**
     * 数据库名称
     */
    @TableField(value = "f_db_name")
    private String dbName;

    /**
     * 模式名称
     */
    @TableField(value = "f_db_schema")
    private String dbSchema;

    /**
     * 表名称
     */
    @TableField(value = "f_tb_name")
    private String tbName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineageTagTableEntity)) return false;
        LineageTagTableEntity table = (LineageTagTableEntity) o;
        return Objects.equals(dbType, table.dbType)
                && Objects.equals(jdbcUrl, table.jdbcUrl)
                && Objects.equals(jdbcUser, table.jdbcUser)
                && Objects.equals(dbName, table.dbName)
                && Objects.equals(dbSchema, table.dbSchema)
                && Objects.equals(tbName, table.tbName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbType, jdbcUrl, jdbcUser, dbName, dbSchema, tbName);
    }

    @Override
    public String toString() {
        return "LineageTagTableEntity[" +
                "dbType:" + (dbType == null ? "" : dbType) +
                ",jdbcUrl:" + (jdbcUrl == null ? "" : jdbcUrl) +
                ",jdbcUser:" + (jdbcUser == null ? "" : jdbcUser) +
                ",dbName:" + (dbName == null ? "" : dbName) +
                ",dbSchema:" + (dbSchema == null ? "" : dbSchema) +
                ",tbName:" + (tbName == null ? "" : tbName) +
                "]";
    }


}
