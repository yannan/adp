package com.eisoo.metadatamanage.web.extra.service;

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.dto.SQLServerDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.AbstractDataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.BaseConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.SQLServerConnectionParam;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.google.auto.service.AutoService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.service
 * @Date: 2023/5/5 14:54
 */
@AutoService(DataSourceProcessor.class)
public class SQLServerDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, SQLServerDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        SQLServerConnectionParam connectionParams = (SQLServerConnectionParam) createConnectionParams(connectionJson);
        String[] hostSeperator = connectionParams.getAddress().split(Constants.DOUBLE_SLASH);
        String[] hostPortArray = hostSeperator[hostSeperator.length - 1].split(Constants.COMMA);

        SQLServerDataSourceParamDTO sqlServerDatasourceParamDTO = new SQLServerDataSourceParamDTO();
        sqlServerDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        sqlServerDatasourceParamDTO.setUserName(connectionParams.getUser());
        sqlServerDatasourceParamDTO.setOther(parseOther(connectionParams.getOther()));
        sqlServerDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));
        sqlServerDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);
        return sqlServerDatasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        SQLServerDataSourceParamDTO sqlServerParam = (SQLServerDataSourceParamDTO) datasourceParam;
        String address =
                String.format("%s%s:%s", DataSourceConstants.JDBC_SQLSERVER, sqlServerParam.getHost(),
                        sqlServerParam.getPort());
        String jdbcUrl = address + ";databaseName=" + sqlServerParam.getDatabase();

        SQLServerConnectionParam sqlServerConnectionParam = new SQLServerConnectionParam();
        sqlServerConnectionParam.setAddress(address);
        sqlServerConnectionParam.setDatabase(sqlServerParam.getDatabase());
        sqlServerConnectionParam.setJdbcUrl(jdbcUrl);
        sqlServerConnectionParam.setOther(transformOther(sqlServerParam.getOther()));
        sqlServerConnectionParam.setUser(sqlServerParam.getUserName());
        sqlServerConnectionParam.setPassword(PasswordUtils.encodePassword(sqlServerParam.getPassword()));
        sqlServerConnectionParam.setDriverClassName(getDatasourceDriver());
        sqlServerConnectionParam.setValidationQuery(getValidationQuery());
        sqlServerConnectionParam.setProps(sqlServerParam.getOther());
        return sqlServerConnectionParam;
    }

    @Override
    public BaseConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, SQLServerConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.COM_SQLSERVER_JDBC_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.SQLSERVER_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        SQLServerConnectionParam sqlServerConnectionParam = (SQLServerConnectionParam) connectionParam;

        if (!StringUtils.isEmpty(sqlServerConnectionParam.getOther())) {
            return String.format("%s;%s", sqlServerConnectionParam.getJdbcUrl(), sqlServerConnectionParam.getOther());
        }
        return sqlServerConnectionParam.getJdbcUrl();
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        SQLServerConnectionParam sqlServerConnectionParam = (SQLServerConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        return DriverManager.getConnection(getJdbcUrl(connectionParam), sqlServerConnectionParam.getUser(),
                PasswordUtils.decodePassword(sqlServerConnectionParam.getPassword()));
    }

    @Override
    public DbType getDbType() {
        return DbType.SQLSERVER;
    }

    @Override
    public DataSourceProcessor create() {
        return new SQLServerDataSourceProcessor();
    }

    private String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        otherMap.forEach((key, value) -> stringBuilder.append(String.format("%s=%s;", key, value)));
        return stringBuilder.toString();
    }

    private Map<String, String> parseOther(String other) {
        if (StringUtils.isEmpty(other)) {
            return null;
        }
        Map<String, String> otherMap = new LinkedHashMap<>();
        for (String config : other.split(";")) {
            otherMap.put(config.split("=")[0], config.split("=")[1]);
        }
        return otherMap;
    }
}
