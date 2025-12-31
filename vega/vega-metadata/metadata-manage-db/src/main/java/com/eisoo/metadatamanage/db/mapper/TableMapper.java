package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.metadatamanage.db.entity.TableEntity;
import com.eisoo.metadatamanage.lib.dto.DataSourceRowsDTO;
import com.eisoo.metadatamanage.lib.dto.SchemaRowsDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TableMapper extends BaseMapper<TableEntity> {
    @Select("SELECT SUM(f_table_rows) AS total_rows FROM t_table")
    Long getTotalRows();

    @Select("SELECT SUM(f_table_rows) AS schemaRows, f_schema_name AS schemaName, f_schema_id AS schemaId FROM t_table GROUP BY f_schema_name, f_schema_id")
    List<SchemaRowsDTO> getSchemaRows();

    @Select("SELECT SUM(f_table_rows) AS dataSourceRows, f_data_source_name AS dataSourceName, f_data_source_id AS dataSourceId FROM t_table GROUP BY f_data_source_name, f_data_source_id")
    List<DataSourceRowsDTO> getDataSourceRows();

    List<TableEntity> queryByDsIdDbSchemaTbName(@Param("dsIdList") List<Long> dsIdList, @Param("schemaList") List<String> schemaList, @Param("tableList") List<String> tableList);
}
