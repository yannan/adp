package com.eisoo.metadatamanage.web.extra.service;

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.dto.OracleDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbConnectType;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.AbstractDataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.BaseConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.OracleConnectionParam;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.service
 * @Date: 2023/4/10 10:19
 */
public class OracleDataSourceProcessor extends AbstractDataSourceProcessor {
    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, OracleDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        OracleConnectionParam connectionParams = (OracleConnectionParam) createConnectionParams(connectionJson);
        OracleDataSourceParamDTO oracleDatasourceParamDTO = new OracleDataSourceParamDTO();

        oracleDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        oracleDatasourceParamDTO.setUserName(connectionParams.getUser());
        oracleDatasourceParamDTO.setOther(parseOther(connectionParams.getOther()));

        String hostSeperator = Constants.DOUBLE_SLASH;
        if (DbConnectType.ORACLE_SID.equals(connectionParams.getConnectType())) {
            hostSeperator = Constants.AT_SIGN;
        }
        String[] hostPort = connectionParams.getAddress().split(hostSeperator);
        String[] hostPortArray = hostPort[hostPort.length - 1].split(Constants.COMMA);
        oracleDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));
        oracleDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);

        return oracleDatasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        OracleDataSourceParamDTO oracleParam = (OracleDataSourceParamDTO) datasourceParam;
        String address;
        String jdbcUrl;
        if (DbConnectType.ORACLE_SID.equals(oracleParam.getConnectType())) {
            address = String.format("%s%s:%s",
                    DataSourceConstants.JDBC_ORACLE_SID, oracleParam.getHost(), oracleParam.getPort());
            jdbcUrl = address + ":" + oracleParam.getDatabase();
        } else {
            address = String.format("%s%s:%s",
                    DataSourceConstants.JDBC_ORACLE_SERVICE_NAME, oracleParam.getHost(), oracleParam.getPort());
            jdbcUrl = address + "/" + oracleParam.getDatabase();
        }

        OracleConnectionParam oracleConnectionParam = new OracleConnectionParam();
        oracleConnectionParam.setUser(oracleParam.getUserName());
        oracleConnectionParam.setPassword(PasswordUtils.encodePassword(oracleParam.getPassword()));
        oracleConnectionParam.setAddress(address);
        oracleConnectionParam.setJdbcUrl(jdbcUrl);
        oracleConnectionParam.setDatabase(oracleParam.getDatabase());
        oracleConnectionParam.setConnectType(oracleParam.getConnectType());
        oracleConnectionParam.setDriverClassName(getDatasourceDriver());
        oracleConnectionParam.setValidationQuery(getValidationQuery());
        oracleConnectionParam.setOther(transformOther(oracleParam.getOther()));
        oracleConnectionParam.setProps(oracleParam.getOther());

        return oracleConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, OracleConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.COM_ORACLE_JDBC_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.ORACLE_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        OracleConnectionParam oracleConnectionParam = (OracleConnectionParam) connectionParam;
        if (!StringUtils.isEmpty(oracleConnectionParam.getOther())) {
            return String.format("%s?%s", oracleConnectionParam.getJdbcUrl(), oracleConnectionParam.getOther());
        }
        return oracleConnectionParam.getJdbcUrl();
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        OracleConnectionParam oracleConnectionParam = (OracleConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        return DriverManager.getConnection(getJdbcUrl(connectionParam),
                oracleConnectionParam.getUser(), PasswordUtils.decodePassword(oracleConnectionParam.getPassword()));
    }

    @Override
    public DbType getDbType() {
        return DbType.ORACLE;
    }

    @Override
    public DataSourceProcessor create() {
        return new OracleDataSourceProcessor();
    }

    private String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        List<String> list = new ArrayList<>();
        otherMap.forEach((key, value) -> list.add(String.format("%s=%s", key, value)));
        return String.join("&", list);
    }

    private Map<String, String> parseOther(String other) {
        if (StringUtils.isEmpty(other)) {
            return null;
        }
        Map<String, String> otherMap = new LinkedHashMap<>();
        String[] configs = other.split("&");
        for (String config : configs) {
            otherMap.put(config.split("=")[0], config.split("=")[1]);
        }
        return otherMap;
    }
}
