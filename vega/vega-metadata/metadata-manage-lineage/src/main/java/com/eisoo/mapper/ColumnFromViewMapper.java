package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.ColumnLineageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;

/**
 *
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:57
 * @Version:1.0
 */
@Mapper
public interface ColumnFromViewMapper<D> extends BaseMapper {
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(ColumnLineageEntity.class)
    void selectColumnLineageBatchFromView(ResultHandler<ColumnLineageEntity> handler);
}
