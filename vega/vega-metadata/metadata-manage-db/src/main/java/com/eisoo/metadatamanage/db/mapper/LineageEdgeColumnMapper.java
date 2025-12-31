package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eisoo.metadatamanage.db.entity.LineageEdgeColumnEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;


/**
 * 字段血缘关系表
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-10 15:08:17
 */
@Mapper
public interface LineageEdgeColumnMapper extends BaseMapper<LineageEdgeColumnEntity> {

    void batchSave(@Param("list") List<LineageEdgeColumnEntity> edgeColList);
}
