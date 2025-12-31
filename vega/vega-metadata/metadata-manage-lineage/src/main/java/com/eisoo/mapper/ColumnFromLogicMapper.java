package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.ColumnLineageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;

/**
 *
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:57
 * @Version:1.0
 */
@Mapper
public interface ColumnFromLogicMapper<D> extends BaseMapper {
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(String.class)
    @Select("  select fvf.id  from adp.form_view fv\n" +
            "    join   adp.form_view_field fvf on fv.id=fvf.form_view_id\n" +
            "    where fv.`type` in (2,3) and fv.deleted_at=0")
    void selectColumnIdBatchFromLogic(ResultHandler<String> handler);
}
