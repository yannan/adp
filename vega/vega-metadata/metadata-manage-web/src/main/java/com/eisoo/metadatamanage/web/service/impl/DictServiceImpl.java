package com.eisoo.metadatamanage.web.service.impl;

import java.util.*;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eisoo.metadatamanage.db.entity.DictEntity;
import com.eisoo.metadatamanage.db.mapper.DictMapper;
import com.eisoo.metadatamanage.lib.vo.DictItemVo;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.metadatamanage.web.service.IDictService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

import lombok.Getter;

import javax.annotation.PostConstruct;

@Service
public class DictServiceImpl extends MPJBaseServiceImpl<DictMapper, DictEntity> implements IDictService {
    @Autowired(required = false)
    DictMapper dictMapper;

    @Getter
    private Map<Integer, Map<Integer, String>> keyToValMap;

    @Getter
    private Map<Integer, Map<String, Integer>> valToKeyMap;

    private Map<Integer, List<DictItemVo>> dictCacheMap;

    private List<DictItemVo> dictCache;

    @Override
    public Result<List<DictItemVo>> getListByDictType(Integer dictType) {
        List<DictItemVo> dicts = getListFromCache(dictType);
        if (dicts != null && !dicts.isEmpty()) {
            return Result.success(dicts);
        }
        throw new AiShuException(ErrorCodeEnum.NotFound, null, Messages.MESSAGE_DATANOTEXIST_ERROR_SOLUTION);
    }

    @Override
    public List<DictItemVo> getList(Integer dictType) {
        MPJLambdaWrapper<DictEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(DictEntity::getDictType, DictEntity::getDictKey, DictEntity::getDictValue);
        wrapper.eq(dictType != null, DictEntity::getDictType, dictType);
        wrapper.eq(DictEntity::getEnableStatus, 1);
        wrapper.orderByAsc(DictEntity::getDictType, DictEntity::getDictKey);
        return selectJoinList(DictItemVo.class, wrapper);
    }

    @Override
    public DictEntity getByJdbcType(Integer jdbcDataType, List<DictEntity> dictItemList) {
        return dictItemList.stream().filter(dictItem -> {
            String extendProperty = dictItem.getExtendProperty();
            JsonNode jsonNode = JSONUtils.toJsonNode(extendProperty);
            return String.valueOf(jdbcDataType).equals(JSONUtils.findValue(jsonNode, DataSourceConstants.JDBC_DATA_TYPE));
        }).findFirst().orElse(null);
    }

    @PostConstruct
    public void initDictCache() {
        dictCache = getList(null);
        keyToValMap = new HashMap<>();
        valToKeyMap = new HashMap<>();
        dictCacheMap = new HashMap<>();

        for (DictItemVo dict : dictCache) {
            dict.setDictValue(dict.getDictValue().toUpperCase());
            Map<Integer, String> vkv = keyToValMap.get(dict.getDictType());
            Map<String, Integer> vvk = valToKeyMap.get(dict.getDictType());
            List<DictItemVo> lv = dictCacheMap.get(dict.getDictType());
            if (vkv == null) {
                vkv = new HashMap<>();
                vvk = new HashMap<>();
                lv = new ArrayList<>();
                keyToValMap.put(dict.getDictType(), vkv);
                valToKeyMap.put(dict.getDictType(), vvk);
                dictCacheMap.put(dict.getDictType(), lv);
            }
            vkv.put(dict.getDictKey(), dict.getDictValue());
            vvk.put(dict.getDictValue(), dict.getDictKey());
            lv.add(dict);
        }
    }

    @Override
    public List<DictItemVo> getListFromCache(Integer dictType) {
        if (dictType != null) {
            return dictCacheMap.get(dictType);
        }
        return dictCache;
    }

    @Override
    public String getDictValue(Integer dictType, Integer dictKey) {
        if (keyToValMap.containsKey(dictType)) {
            return keyToValMap.get(dictType).get(dictKey);
        }
        return null;
    }

    @Override
    public Integer getDictKey(Integer dictType, String dictValue) {
        if (valToKeyMap.containsKey(dictType)) {
            return valToKeyMap.get(dictType).get(dictValue);
        }
        return null;
    }

    @Override
    public Set<Integer> getDictKeySet(Integer dictType) {
        if (keyToValMap.containsKey(dictType)) {
            return keyToValMap.get(dictType).keySet();
        }
        return null;
    }

    @Override
    public Set<String> getDictValueSet(Integer dictType) {
        if (valToKeyMap.containsKey(dictType)) {
            return valToKeyMap.get(dictType).keySet();
        }
        return null;
    }

    @Override
    public boolean containsKey(Integer dictType, Integer dictKey) {
        if (keyToValMap.containsKey(dictType)) {
            return keyToValMap.get(dictType).containsKey(dictKey);
        }
        return false;
    }
}
