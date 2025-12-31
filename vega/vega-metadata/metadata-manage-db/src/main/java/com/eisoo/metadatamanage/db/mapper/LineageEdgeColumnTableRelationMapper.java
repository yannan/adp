package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eisoo.metadatamanage.db.entity.LineageEdgeColumnTableRelationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;


/**
 * 表字段关系映射表
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Mapper
public interface LineageEdgeColumnTableRelationMapper extends BaseMapper<LineageEdgeColumnTableRelationEntity> {

    void batchSave(@Param("list") List<LineageEdgeColumnTableRelationEntity> edgeTableColumnRelationList);
}
