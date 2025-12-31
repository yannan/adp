package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.entity.DolphinEntity;
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
public interface DolphinLineageMapper extends BaseMapper<ColumnLineageEntity> {
    Integer insertBatchColumn(@Param("list")  List<ColumnLineageEntity> entityList);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(DolphinEntity.class)
    @Select("select id,source_table_id,target_table_id  from af_business.data_collecting_model where deleted_at =0")
    void selectDolphinSyncEntity(ResultHandler<DolphinEntity> handler);
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(DolphinEntity.class)
    @Select("select  id,create_sql,insert_sql from af_business.data_processing_model where deleted_at =0")
    void selectDolphinComposeEntity(ResultHandler<DolphinEntity> handler);
}
