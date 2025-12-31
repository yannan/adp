package com.eisoo.mapper;


import com.eisoo.lineage.CommonUtil;
import com.eisoo.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/12 17:02
 * @Version:1.0
 */
@MappedTypes(value = {Set.class })
@MappedJdbcTypes(value = JdbcType.VARCHAR)
public  class SetTypeHandler extends BaseTypeHandler<Set<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Set<String> parameter, JdbcType jdbcType) throws SQLException {
        String string = parameter == null || parameter.isEmpty() ? "" : StringUtils.join(parameter, Constant.GLOBAL_SPLIT_COMMA);
        preparedStatement.setString(i, string);
    }
    @Override
    public Set<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String reString = resultSet.getString(columnName);
        if (CommonUtil.isEmpty(reString)){
            return new HashSet<>();
        }
        return Arrays.stream(reString.split(Constant.GLOBAL_SPLIT_COMMA)).collect(Collectors.toSet());
    }
    @Override
    public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String reString = rs.getString(columnIndex);
        if (CommonUtil.isEmpty(reString)){
            return new HashSet<>();
        }
        return  Arrays.stream(reString.split(Constant.GLOBAL_SPLIT_COMMA)).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String reString = cs.getString(columnIndex);
        if (CommonUtil.isEmpty(reString)){
            return new HashSet<>();
        }
        return  Arrays.stream(reString.split(Constant.GLOBAL_SPLIT_COMMA)).collect(Collectors.toSet());
    }
}


