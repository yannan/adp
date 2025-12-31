package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.metadatamanage.db.entity.LineageTagColumnEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 字段信息
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Mapper
public interface LineageTagColumnMapper extends BaseMapper<LineageTagColumnEntity> {

    void batchSave(@Param("list") List<LineageTagColumnEntity> tagColList);
}
