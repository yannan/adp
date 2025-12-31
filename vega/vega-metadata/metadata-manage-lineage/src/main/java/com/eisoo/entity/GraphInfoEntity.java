package com.eisoo.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/8 9:18
 * @Version:1.0
 */
@Data
@ToString
@TableName(value = "t_lineage_graph_info")
@AllArgsConstructor
@NoArgsConstructor
public class GraphInfoEntity {
    @TableId(value = "app_id")
    private String appId;
    @TableField("graph_id")
    private Integer graphId;
}
