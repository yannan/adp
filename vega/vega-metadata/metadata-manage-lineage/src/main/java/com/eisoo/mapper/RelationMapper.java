package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.RelationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:57
 * @Version:1.0
 */
@Mapper
public interface RelationMapper extends BaseMapper<RelationEntity> {
    void truncateTable();
    void insertBatchColumnRelation();
    void insertBatchIndicatorRelation();
    void saveOrUpdateRelation(RelationEntity relationEntity);
    Integer insertBatchSomeColumn(@Param("list") List<RelationEntity> entityList);
    Integer updateChildBatchChildField(@Param("list") List<RelationEntity> entityList);
    Integer insertBatchSomeColumnNotDep(@Param("list") List<RelationEntity> entityList);
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(RelationEntity.class)
    void selectRelationEntity(ResultHandler<RelationEntity> handler);
}
