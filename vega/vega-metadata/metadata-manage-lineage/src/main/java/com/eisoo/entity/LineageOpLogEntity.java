package com.eisoo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/26 14:18
 * @Version:1.0
 */
@TableName(value = "t_lineage_log")
@Data
public class LineageOpLogEntity {
    @TableId(value = "id")
    private String id;
    @TableField(value = "class_id")
    private String classId;
    @TableField(value = "class_type")
    private String classType;
    @TableField(value = "action_type")
    private String actionType;
    @TableField(value = "class_data")
    private String classData;
    @TableField(value = "created_at")
    private String createdAt = new java.sql.Timestamp(System.currentTimeMillis()).toString();

    public LineageOpLogEntity(String type, String actionType, String jsonString) {
        this.classType=type;
        this.actionType=actionType;
        this.classData=jsonString;
    }
}
