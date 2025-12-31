package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.DataSourceEntityDataConnection;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Delete;

public interface DataConnectionDataSourceMapper extends MPJBaseMapper<DataSourceEntityDataConnection> {
    @Delete("DELETE FROM t_table_field WHERE f_table_id IN (SELECT f_id FROM t_table WHERE f_data_source_id = #{datasourceId})")
    boolean clearColumnsByDsId(String datasourceId);
}
