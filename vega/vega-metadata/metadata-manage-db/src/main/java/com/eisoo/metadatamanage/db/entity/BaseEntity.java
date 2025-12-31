package com.eisoo.metadatamanage.db.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.eisoo.standardization.common.util.AiShuUtil;
import lombok.Data;

@Data
public class BaseEntity {

    /**
     * 数据源的逻辑删除标识码
     */
    @TableField(value = "f_delete_code")
    private String deleteCode;

    @TableField(exist = false)
    Boolean isDeleted;

    public Boolean getIsDeleted(){
        if(AiShuUtil.isEmpty(deleteCode) || deleteCode.equals("0l")) {
            return false;
        } else {
            return true;
        }
    }
}
