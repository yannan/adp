package com.eisoo.metadatamanage.db.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class LineageParentEntity {
    /**
     * 创建时间，时间戳
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 修改时间，时间戳
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    /**
     * 删除时间，时间戳
     */
    @TableField(value = "deleted_at")
    private Long deletedAt;
}
