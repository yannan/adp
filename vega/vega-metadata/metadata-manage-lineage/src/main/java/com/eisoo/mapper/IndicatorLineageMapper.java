package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.IndicatorLineageEntity;
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
public interface IndicatorLineageMapper extends BaseMapper<IndicatorLineageEntity> {
    void truncateTable();

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(String.class)
    @Select("select id from  af_data_model.t_technical_indicator  where indicator_type='atomic' and (deleted_at=1 or deleted_at is null)")
    void selectIndicatorId(ResultHandler<String> handler);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(IndicatorLineageEntity.class)
    @Select("select * from  t_lineage_tag_indicator2")
    void selectIndicatorLineageEntity(ResultHandler<IndicatorLineageEntity> handler);

    Integer insertBatchSomeColumn(@Param("list") List<IndicatorLineageEntity> entityList);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(IndicatorLineageEntity.class)
    void selectDepsInfo(ResultHandler<IndicatorLineageEntity> handler);
}
