package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.DipDataSourceEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Delete;

public interface DipDataSourceMapper extends MPJBaseMapper<DipDataSourceEntity> {
    @Delete("DELETE FROM t_table_field WHERE f_table_id IN (SELECT f_id FROM t_table WHERE f_data_source_id = #{datasourceId})")
    boolean clearColumnsByDsId(String datasourceId);
}
