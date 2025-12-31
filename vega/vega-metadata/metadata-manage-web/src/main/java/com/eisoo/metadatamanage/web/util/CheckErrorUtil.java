package com.eisoo.metadatamanage.web.util;

import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.exception.UnauthorizedException;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class CheckErrorUtil {

    public static void checkUnauthorized(String token){
        if (null == token || "".equals(token)){
            throw new UnauthorizedException();
        }
    }
    public static void checkDsId(String dsId){
        if (null == dsId || "".equals(dsId)){
            throw new  AiShuException(ErrorCodeEnum.InvalidParameter, "dsId不能是空");
        }
    }

    public static List<CheckErrorVo> createError(String field, String errorDesc, List<CheckErrorVo> errorList) {
        if (errorList == null) {
            errorList = new ArrayList<>();
        }
        CheckErrorVo error = new CheckErrorVo(field, errorDesc);
        errorList.add(error);
        return errorList;
    }

    public static List<CheckErrorVo> createError(String field, String errorDesc) {
        return createError(field, errorDesc, null);
    }

    public static List<CheckErrorVo> getCheckList(Integer offset, Integer limit, String sort, List<String> sortList, String direction, List<String> directionList) {
        List<CheckErrorVo> detail = Lists.newArrayList();
        String key;
        String message;
        if (offset < 1) {
            key = Constants.PARAMETER_OFFSET;
            message = Messages.MESSAGE_POSITIVE_INTEGER;
            detail.add(new CheckErrorVo(key, message));
        }
        if (limit < 1 || limit > 1000) {
            key = Constants.PARAMETER_LIMIT;
            message = Messages.MESSAGE_POSITIVE_INTEGER_1000;
            detail.add(new CheckErrorVo(key, message));
        }
        if (!sortList.contains(sort)) {
            key = Constants.PARAMETER_SORT;
            message = Messages.MESSAGE_VALUE_NO_VALID;
            detail.add(new CheckErrorVo(key, message));
        }
        if (!directionList.contains(direction)) {
            key = Constants.PARAMETER_DIRECTION;
            message = Messages.MESSAGE_VALUE_NO_VALID;
            detail.add(new CheckErrorVo(key, message));
        }
        return  detail;
    }

    public static void checkSelectListParameter(Integer offset, Integer limit, String sort, List<String> sortList, String direction, List<String> directionList) {
        List<CheckErrorVo> parameterErrors = getCheckList(offset, limit, sort, sortList, direction, directionList);
        if (AiShuUtil.isNotEmpty(parameterErrors)) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, parameterErrors);
        }
    }

    public static void checkPositiveLong(Object value, String key) {
        Long num = ConvertUtil.toLong(value);
        List<CheckErrorVo> detail = Lists.newArrayList();
        String message;
        if (AiShuUtil.isEmpty(num) || num < 1) {
            message = Messages.MESSAGE_POSITIVE_INTEGER;
            detail.add(new CheckErrorVo(key, message));
        }
        if (AiShuUtil.isNotEmpty(detail)) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, detail);
        }
    }
}
