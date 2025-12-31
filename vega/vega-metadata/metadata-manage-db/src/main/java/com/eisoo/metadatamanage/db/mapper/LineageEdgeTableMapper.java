package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eisoo.metadatamanage.db.entity.LineageEdgeTableEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;


/**
 * 表血缘关系
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Mapper
public interface LineageEdgeTableMapper extends BaseMapper<LineageEdgeTableEntity> {

    void batchSave(@Param("list") List<LineageEdgeTableEntity> edgeTbList);
}
