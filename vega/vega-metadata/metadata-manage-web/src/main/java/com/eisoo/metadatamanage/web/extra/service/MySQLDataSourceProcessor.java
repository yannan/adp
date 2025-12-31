package com.eisoo.metadatamanage.web.extra.service;


import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.dto.MySQLDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.AbstractDataSourceProcessor;
import com.eisoo.metadatamanage.web.extra.model.BaseConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.MySQLConnectionParam;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.google.auto.service.AutoService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.service
 * @Date: 2023/3/31 14:27
 */

@AutoService(DataSourceProcessor.class)
public class MySQLDataSourceProcessor extends AbstractDataSourceProcessor {
    private final Logger logger = LoggerFactory.getLogger(MySQLDataSourceProcessor.class);

    private static final String ALLOW_LOAD_LOCAL_IN_FILE_NAME = "allowLoadLocalInfile";

    private static final String AUTO_DESERIALIZE = "autoDeserialize";

    private static final String ALLOW_LOCAL_IN_FILE_NAME = "allowLocalInfile";

    private static final String ALLOW_URL_IN_LOCAL_IN_FILE_NAME = "allowUrlInLocalInfile";

    private static final String APPEND_PARAMS = "allowLoadLocalInfile=false&autoDeserialize=false&allowLocalInfile=false&allowUrlInLocalInfile=false";

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, MySQLDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        MySQLConnectionParam
                connectionParams = (MySQLConnectionParam) createConnectionParams(connectionJson);
        MySQLDataSourceParamDTO
                mysqlDatasourceParamDTO = new MySQLDataSourceParamDTO();

        mysqlDatasourceParamDTO.setUserName(connectionParams.getUser());
        mysqlDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        mysqlDatasourceParamDTO.setOther(parseOther(connectionParams.getOther()));

        String address = connectionParams.getAddress();
        String[] hostSeperator = address.split(Constants.DOUBLE_SLASH);
        String[] hostPortArray = hostSeperator[hostSeperator.length - 1].split(Constants.COMMA);
        mysqlDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));
        mysqlDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);

        return mysqlDatasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        MySQLDataSourceParamDTO mysqlDatasourceParam = (MySQLDataSourceParamDTO) dataSourceParam;
        //Todo 先写死在字典类里面，以后再动态加载
        String address = String.format("%s%s:%s", DataSourceConstants.JDBC_MYSQL, mysqlDatasourceParam.getHost(),
                mysqlDatasourceParam.getPort());
        String jdbcUrl = String.format("%s/%s", address, mysqlDatasourceParam.getDatabase());

        MySQLConnectionParam
                mysqlConnectionParam = new MySQLConnectionParam();
        mysqlConnectionParam.setJdbcUrl(jdbcUrl);
        mysqlConnectionParam.setDatabase(mysqlDatasourceParam.getDatabase());
        mysqlConnectionParam.setAddress(address);
        mysqlConnectionParam.setUser(mysqlDatasourceParam.getUserName());
        mysqlConnectionParam.setPassword(PasswordUtils.encodePassword(mysqlDatasourceParam.getPassword()));
        mysqlConnectionParam.setDriverClassName(getDatasourceDriver());
        mysqlConnectionParam.setValidationQuery(getValidationQuery());
        mysqlConnectionParam.setOther(transformOther(mysqlDatasourceParam.getOther()));
        mysqlConnectionParam.setProps(mysqlDatasourceParam.getOther());

        return mysqlConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, MySQLConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.COM_MYSQL_CJ_JDBC_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.MYSQL_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        MySQLConnectionParam
                mysqlConnectionParam = (MySQLConnectionParam) connectionParam;
        String jdbcUrl = mysqlConnectionParam.getJdbcUrl();
        if (!StringUtils.isEmpty(mysqlConnectionParam.getOther())) {
            return String.format("%s?%s&%s", jdbcUrl, mysqlConnectionParam.getOther(), APPEND_PARAMS);
        }
        return String.format("%s?%s", jdbcUrl, APPEND_PARAMS);
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        MySQLConnectionParam mysqlConnectionParam = (MySQLConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        String user = mysqlConnectionParam.getUser();
        if (user.contains(AUTO_DESERIALIZE)) {
            logger.warn("sensitive param : {} in username field is filtered", AUTO_DESERIALIZE);
            user = user.replace(AUTO_DESERIALIZE, "");
        }
        String password = PasswordUtils.decodePassword(mysqlConnectionParam.getPassword());
        if (password.contains(AUTO_DESERIALIZE)) {
            logger.warn("sensitive param : {} in password field is filtered", AUTO_DESERIALIZE);
            password = password.replace(AUTO_DESERIALIZE, "");
        }
        logger.info("connectionParam:"+connectionParam);
        logger.info("getJdbcUrl(connectionParam):"+getJdbcUrl(connectionParam));
        return DriverManager.getConnection(getJdbcUrl(connectionParam), user, password);
    }

    @Override
    public DbType getDbType() {
        return DbType.MYSQL;
    }

    @Override
    public DataSourceProcessor create() {
        return new MySQLDataSourceProcessor();
    }

    private String transformOther(Map<String, String> paramMap) {
        if (MapUtils.isEmpty(paramMap)) {
            return null;
        }
        Map<String, String> otherMap = new HashMap<>();
        paramMap.forEach((k, v) -> {
            if (!checkKeyIsLegitimate(k)) {
                return;
            }
            otherMap.put(k, v);
        });
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        otherMap.forEach((key, value) -> stringBuilder.append(String.format("%s=%s&", key, value)));
        return stringBuilder.toString();
    }

    private static boolean checkKeyIsLegitimate(String key) {
        return !key.contains(ALLOW_LOAD_LOCAL_IN_FILE_NAME)
                && !key.contains(AUTO_DESERIALIZE)
                && !key.contains(ALLOW_LOCAL_IN_FILE_NAME)
                && !key.contains(ALLOW_URL_IN_LOCAL_IN_FILE_NAME);
    }

    private Map<String, String> parseOther(String other) {
        if (StringUtils.isEmpty(other)) {
            return null;
        }
        Map<String, String> otherMap = new LinkedHashMap<>();
        for (String config : other.split("&")) {
            otherMap.put(config.split("=")[0], config.split("=")[1]);
        }
        return otherMap;
    }

}
