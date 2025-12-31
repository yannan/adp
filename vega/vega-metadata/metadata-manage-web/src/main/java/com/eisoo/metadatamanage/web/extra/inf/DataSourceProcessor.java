package com.eisoo.metadatamanage.web.extra.inf;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.inf
 * @Date: 2023/3/30 15:23
 */

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public interface DataSourceProcessor {
    /**
     * cast JSON to relate DTO
     *
     * @param paramJson
     * @return {@link BaseDataSourceParamDTO}
     */
    BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson);


    /**
     * check datasource param is valid
     */
    void checkDatasourceParam(BaseDataSourceParamDTO datasourceParam);

    /**
     * get Datasource Client UniqueId
     *
     * @return UniqueId
     */
    String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType);

    /**
     * create BaseDataSourceParamDTO by connectionJson
     *
     * @param connectionJson
     * @return {@link BaseDataSourceParamDTO}
     */
    BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson);

    /**
     * create datasource connection parameter which will be stored at DataSource
     * <p>
     * see {@code org.apache.dolphinscheduler.dao.entity.DataSource.connectionParams}
     */
    ConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam);

    /**
     * deserialize json to datasource connection param
     *
     * @param connectionJson {@code org.apache.dolphinscheduler.dao.entity.DataSource.connectionParams}
     * @return
     */
    ConnectionParam createConnectionParams(String connectionJson);

    /**
     * get datasource Driver
     */
    String getDatasourceDriver();

    /**
     * get validation Query
     */
    String getValidationQuery();

    /**
     * get jdbcUrl by connection param, the jdbcUrl is different with ConnectionParam.jdbcUrl, this method will inject
     * other to jdbcUrl
     *
     * @param connectionParam connection param
     */
    String getJdbcUrl(ConnectionParam connectionParam);

    /**
     * get connection by connectionParam
     *
     * @param connectionParam connectionParam
     * @return {@link Connection}
     */
    Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException, IOException;

    /**
     * @return {@link DbType}
     */
    DbType getDbType();

    /**
     * get datasource processor
     */
    DataSourceProcessor create();
}
