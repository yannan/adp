package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.TableFieldEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TableFieldMapper extends MPJBaseMapper<TableFieldEntity> {
    List<TableFieldEntity> queryByTableIdList(@Param("tbIdList") List<Long> tbIdList);
    @Update("UPDATE t_table_field SET f_delete_flag = 1, f_delete_time = current_timestamp() where f_table_id = #{tableId}")
    int deleteByTableId(Long tableId);
}
