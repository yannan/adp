package com.eisoo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eisoo.mapper.SetTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/12 14:24
 * @Version:1.0
 */
@Data
@NoArgsConstructor
@TableName(value = "t_lineage_relation")
public class RelationEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "unique_id")
    private String uniqueId;
    @TableField("class_type")
    private Integer classType;
    @TableField(typeHandler = SetTypeHandler.class)
    private Set<String> parent;
    @TableField(typeHandler = SetTypeHandler.class)
    private Set<String> child;
    public RelationEntity(String uniqueId,Integer classType){
        this.uniqueId = uniqueId;
        this.classType = classType;
    }
}
