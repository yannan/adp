package com.eisoo.metadatamanage.web.extra.service;

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.dto.ClickHouseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.AbstractDataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.ClickHouseConnectionParam;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.google.auto.service.AutoService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@AutoService(DataSourceProcessor.class)
public class ClickHouseDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, ClickHouseDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        ClickHouseConnectionParam connectionParams = (ClickHouseConnectionParam) createConnectionParams(connectionJson);

        ClickHouseDataSourceParamDTO
                clickHouseDatasourceParamDTO = new ClickHouseDataSourceParamDTO();
        clickHouseDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        clickHouseDatasourceParamDTO.setUserName(connectionParams.getUser());
        clickHouseDatasourceParamDTO.setOther(parseOther(connectionParams.getOther()));

        String[] hostSeperator = connectionParams.getAddress().split(Constants.DOUBLE_SLASH);
        String[] hostPortArray = hostSeperator[hostSeperator.length - 1].split(Constants.COMMA);
        clickHouseDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));
        clickHouseDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);

        return clickHouseDatasourceParamDTO;
    }

    @Override
    public ConnectionParam createConnectionParams(BaseDataSourceParamDTO datasourceParam) {
        ClickHouseDataSourceParamDTO clickHouseParam = (ClickHouseDataSourceParamDTO) datasourceParam;
        String address;
        if (clickHouseParam.getHost().contains(Constants.COMMA)) {
            List<String> hostList = Arrays.stream(clickHouseParam.getHost().split(Constants.COMMA)).map(s->StringUtils.trim(s)).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder();
            hostList.forEach(host -> {
                stringBuilder.append(host);
                stringBuilder.append(Constants.COLON);
                stringBuilder.append(clickHouseParam.getPort());
                stringBuilder.append(Constants.COMMA);
            });
            stringBuilder.deleteCharAt(stringBuilder.length() -1);
            address = String.format("%s%s", DataSourceConstants.JDBC_CLICKHOUSE, stringBuilder);
        } else {
            address = String.format("%s%s:%s", DataSourceConstants.JDBC_CLICKHOUSE, clickHouseParam.getHost(),
                    clickHouseParam.getPort());
        }

        String jdbcUrl = address + "/" + clickHouseParam.getDatabase();

        ClickHouseConnectionParam clickhouseConnectionParam = new ClickHouseConnectionParam();
        clickhouseConnectionParam.setDatabase(clickHouseParam.getDatabase());
        clickhouseConnectionParam.setAddress(address);
        clickhouseConnectionParam.setJdbcUrl(jdbcUrl);
        clickhouseConnectionParam.setUser(clickHouseParam.getUserName());
        clickhouseConnectionParam.setPassword(PasswordUtils.encodePassword(clickHouseParam.getPassword()));
        clickhouseConnectionParam.setDriverClassName(getDatasourceDriver());
        clickhouseConnectionParam.setValidationQuery(getValidationQuery());
        clickhouseConnectionParam.setOther(transformOther(clickHouseParam.getOther()));
        clickhouseConnectionParam.setProps(clickHouseParam.getOther());
        return clickhouseConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, ClickHouseConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.COM_CLICKHOUSE_JDBC_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.CLICKHOUSE_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        ClickHouseConnectionParam clickhouseConnectionParam = (ClickHouseConnectionParam) connectionParam;
        String jdbcUrl = clickhouseConnectionParam.getJdbcUrl();
        if (!StringUtils.isEmpty(clickhouseConnectionParam.getOther())) {
            jdbcUrl = String.format("%s?%s", jdbcUrl, clickhouseConnectionParam.getOther());
        }
        return jdbcUrl;
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        ClickHouseConnectionParam clickhouseConnectionParam = (ClickHouseConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        return DriverManager.getConnection(getJdbcUrl(clickhouseConnectionParam),
                clickhouseConnectionParam.getUser(), PasswordUtils.decodePassword(clickhouseConnectionParam.getPassword()));
    }

    @Override
    public DbType getDbType() {
        return DbType.CLICKHOUSE;
    }

    @Override
    public DataSourceProcessor create() {
        return new ClickHouseDataSourceProcessor();
    }

    private String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        otherMap.forEach((key, value) -> stringBuilder.append(String.format("%s=%s%s", key, value, "&")));
        return stringBuilder.toString();
    }

    private Map<String, String> parseOther(String other) {
        if (other == null) {
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
