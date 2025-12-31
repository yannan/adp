package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.metadatamanage.db.entity.LineageTagTableEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 表信息
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Mapper
public interface LineageTagTableMapper extends BaseMapper<LineageTagTableEntity> {

    void batchSave(@Param("list") List<LineageTagTableEntity> list);
}
