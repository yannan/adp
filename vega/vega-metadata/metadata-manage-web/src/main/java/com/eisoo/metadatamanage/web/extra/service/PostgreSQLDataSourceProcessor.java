package com.eisoo.metadatamanage.web.extra.service;

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.dto.PostgreSQLDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.AbstractDataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.BaseConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.PostgreSQLConnectionParam;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.google.common.base.Strings;
import org.apache.commons.collections4.MapUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.service
 * @Date: 2023/4/12 10:12
 */
public class PostgreSQLDataSourceProcessor extends AbstractDataSourceProcessor {
    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, PostgreSQLDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        PostgreSQLConnectionParam connectionParams = (PostgreSQLConnectionParam) createConnectionParams(connectionJson);
        PostgreSQLDataSourceParamDTO
                postgreSqlDatasourceParamDTO = new PostgreSQLDataSourceParamDTO();
        postgreSqlDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        postgreSqlDatasourceParamDTO.setUserName(connectionParams.getUser());
        postgreSqlDatasourceParamDTO.setOther(parseOther(connectionParams.getOther()));

        String address = connectionParams.getAddress();
        String[] hostSeperator = address.split(Constants.DOUBLE_SLASH);
        String[] hostPortArray = hostSeperator[hostSeperator.length - 1].split(Constants.COMMA);
        postgreSqlDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);
        postgreSqlDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));

        return postgreSqlDatasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        PostgreSQLDataSourceParamDTO postgreSqlParam = (PostgreSQLDataSourceParamDTO) datasourceParam;
        String address = String.format("%s%s:%s", DataSourceConstants.JDBC_POSTGRESQL, postgreSqlParam.getHost(),
                postgreSqlParam.getPort());
        String jdbcUrl = String.format("%s/%s", address, postgreSqlParam.getDatabase());

        PostgreSQLConnectionParam postgreSqlConnectionParam = new PostgreSQLConnectionParam();
        postgreSqlConnectionParam.setJdbcUrl(jdbcUrl);
        postgreSqlConnectionParam.setAddress(address);
        postgreSqlConnectionParam.setDatabase(postgreSqlParam.getDatabase());
        postgreSqlConnectionParam.setUser(postgreSqlParam.getUserName());
        postgreSqlConnectionParam.setPassword(PasswordUtils.encodePassword(postgreSqlParam.getPassword()));
        postgreSqlConnectionParam.setDriverClassName(getDatasourceDriver());
        postgreSqlConnectionParam.setValidationQuery(getValidationQuery());
        postgreSqlConnectionParam.setOther(transformOther(postgreSqlParam.getOther()));
        postgreSqlConnectionParam.setProps(postgreSqlParam.getOther());

        return postgreSqlConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, PostgreSQLConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.ORG_POSTGRESQL_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.POSTGRESQL_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        PostgreSQLConnectionParam postgreSqlConnectionParam = (PostgreSQLConnectionParam) connectionParam;
        if (!Strings.isNullOrEmpty(postgreSqlConnectionParam.getOther())) {
            return String.format("%s?%s", postgreSqlConnectionParam.getJdbcUrl(), postgreSqlConnectionParam.getOther());
        }
        return postgreSqlConnectionParam.getJdbcUrl();
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        PostgreSQLConnectionParam postgreSqlConnectionParam = (PostgreSQLConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        return DriverManager.getConnection(getJdbcUrl(postgreSqlConnectionParam),
                postgreSqlConnectionParam.getUser(), PasswordUtils.decodePassword(postgreSqlConnectionParam.getPassword()));
    }

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public DataSourceProcessor create() {
        return new PostgreSQLDataSourceProcessor();
    }

    private String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        otherMap.forEach((key, value) -> stringBuilder.append(String.format("%s=%s&", key, value)));
        return stringBuilder.toString();
    }

    private Map<String, String> parseOther(String other) {
        if (Strings.isNullOrEmpty(other)) {
            return null;
        }
        Map<String, String> otherMap = new LinkedHashMap<>();
        for (String config : other.split("&")) {
            String[] split = config.split("=");
            otherMap.put(split[0], split[1]);
        }
        return otherMap;
    }
}
