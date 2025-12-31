package com.eisoo.metadatamanage.db.mapper;

import java.util.List;
import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;
public interface DataSourceMapper extends MPJBaseMapper<DataSourceEntity> {
    int deleteByIDs(List<Long> ids);
    @Delete("DELETE FROM t_table_field WHERE f_table_id IN (SELECT f_id FROM t_table WHERE f_data_source_id = #{datasourceId})")
    boolean clearColumnsByDsId(Long datasourceId);
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(DataSourceEntity.class)
    @Select("select f_id,f_data_source_type_name,f_user_name,f_extend_property,f_host,f_port from t_data_source  WHERE f_delete_code = 0")
    void selectAllDataSourceEntity(ResultHandler<DataSourceEntity> handler);
    List<String> getDataSourceList(String dbType, String host, Integer port, String database);
}
