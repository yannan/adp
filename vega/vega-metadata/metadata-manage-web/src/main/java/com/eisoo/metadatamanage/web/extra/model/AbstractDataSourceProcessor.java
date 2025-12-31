package com.eisoo.metadatamanage.web.extra.model;

import com.eisoo.metadatamanage.lib.dto.BaseDataSourceParamDTO;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.MapUtils;

import java.util.regex.Pattern;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.extra.model
 * @Date: 2023/3/31 14:29
 */
public abstract class AbstractDataSourceProcessor implements DataSourceProcessor {
    private static final Pattern IPV4_PATTERN = Pattern.compile("^[a-zA-Z0-9\\_\\-\\.\\,]+$");

    private static final Pattern IPV6_PATTERN = Pattern.compile("^[a-zA-Z0-9\\_\\-\\.\\:\\[\\]\\,]+$");

    private static final Pattern DATABASE_PATTER = Pattern.compile("^[a-zA-Z0-9\\_\\-\\.]+$");

    private static final Pattern PARAMS_PATTER = Pattern.compile("^[a-zA-Z0-9\\-\\_\\/\\@\\.]+$");

    private static final Set<String> POSSIBLE_MALICIOUS_KEYS = Sets.newHashSet("allowLoadLocalInfile");

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO baseDataSourceParamDTO) {
        checkHost(baseDataSourceParamDTO.getHost());
        checkDatabasePatter(baseDataSourceParamDTO.getDatabase());
        checkOther(baseDataSourceParamDTO.getOther());
    }

    /**
     * Check the host is valid
     *
     * @param host datasource host
     */
    protected void checkHost(String host) {
        if (!IPV4_PATTERN.matcher(host).matches() || !IPV6_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("datasource host illegal");
        }
    }

    /**
     * check database name is valid
     *
     * @param database database name
     */
    protected void checkDatabasePatter(String database) {
        if (!DATABASE_PATTER.matcher(database).matches()) {
            throw new IllegalArgumentException("database name illegal");
        }
    }

    /**
     * check other is valid
     *
     * @param other other
     */
    protected void checkOther(Map<String, String> other) {
        if (MapUtils.isEmpty(other)) {
            return;
        }
        if (!Sets.intersection(other.keySet(), POSSIBLE_MALICIOUS_KEYS).isEmpty()) {
            throw new IllegalArgumentException("Other params include possible malicious keys.");
        }
        boolean paramsCheck = other.entrySet().stream().allMatch(p -> PARAMS_PATTER.matcher(p.getValue()).matches());
        if (!paramsCheck) {
            throw new IllegalArgumentException("datasource other params illegal");
        }
    }

    @Override
    public String getDatasourceUniqueId(ConnectionParam connectionParam, DbType dbType) {
        BaseConnectionParam baseConnectionParam = (BaseConnectionParam) connectionParam;
        return MessageFormat.format("{0}@{1}@{2}@{3}", dbType.getDescp(), baseConnectionParam.getUser(),
                PasswordUtils.encodePassword(baseConnectionParam.getPassword()), baseConnectionParam.getJdbcUrl());
    }
}
