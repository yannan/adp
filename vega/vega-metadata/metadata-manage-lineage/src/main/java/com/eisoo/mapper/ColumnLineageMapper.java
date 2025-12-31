package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.ColumnLineageEntity;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:57
 * @Version:1.0
 */
@Mapper
public interface ColumnLineageMapper extends BaseMapper<ColumnLineageEntity> {
    void truncateTable();
    Integer insertBatchSomeColumn(@Param("list") List<ColumnLineageEntity> entityList);
    List<String> selectColumnList(List<String> uniqueIds);
    Integer updateColumnDeps(@Param("list") List<ColumnLineageEntity> entityList);
    Integer removeBatchColumnDeps(@Param("list") List<ColumnLineageEntity> entityList);
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(ColumnLineageEntity.class)
    @Select("select * from t_lineage_tag_column2")
    void selectColumnLineageBatch(ResultHandler<ColumnLineageEntity> handler);
}
