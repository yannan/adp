package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_live_ddl")
public class LiveDdlEntity {
    @TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 数据源唯一标识
     */
    @TableField(value = "f_data_source_id")
    private String dataSourceId;

    /**
     * 冗余字段，数据源名称
     */
    @TableField(value = "f_data_source_name")
    private String dataSourceName;

    /**
     * 物理catalog
     */
    @TableField(value = "f_origin_catalog")
    private String originCatalog;

    /**
     * 虚拟化catalog
     */
    @TableField(value = "f_virtual_catalog")
    private String virtualCatalog;

    /**
     * schema唯一标识
     */
    @TableField(value = "f_schema_id")
    private Long schemaId;

    /**
     * 冗余字段，schema名称
     */
    @TableField(value = "f_schema_name")
    private String schemaName;

    /**
     * 表唯一标识
     */
//    @TableField(value = "f_table_id")
//    private Long tableId;

    /**
     * 冗余字段，表名称
     */
    @TableField(value = "f_table_name")
    private String tableName;

    /**
     * sql类型（待补全）
     */
    @TableField(value = "f_sql_type")
    private String sqlType;

    /**
     * sql文本
     */
    @TableField(value = "f_sql_text")
    private String sqlText;

    /**
     * 监听时间，默认当前时间
     */
    @TableField(value = "f_monitor_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date monitorTime;

    /**
     * 更新结果
     */
    @TableField(value = "f_update_status")
    private Integer updateStatus;

    /**
     * 更新信息
     */
    @TableField(value = "f_update_message")
    private String updateMessage;

    /**
     * 推送状态
     */
    @TableField(value = "f_push_status")
    private Integer pushStatus;

}
