package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.TableLineageEntity;
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
public interface TableLineageMapper extends BaseMapper<TableLineageEntity> {
    void truncateTable();
    Integer insertBatchSomeColumn(@Param("list") List<TableLineageEntity> entityList);
    List<String> selectTableUniqueIdList(List<String> uniqueIds);
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(TableLineageEntity.class)
    @Select("select * from t_lineage_tag_table2")
    void selectTableLineageBatch(ResultHandler<TableLineageEntity> handler);
}
