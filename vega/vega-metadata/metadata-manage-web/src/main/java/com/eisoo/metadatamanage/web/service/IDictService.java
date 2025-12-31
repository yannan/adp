package com.eisoo.metadatamanage.web.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eisoo.metadatamanage.db.entity.DictEntity;
import com.eisoo.metadatamanage.lib.vo.DictItemVo;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseService;

public interface IDictService extends MPJBaseService<DictEntity> {
    Result<List<DictItemVo>> getListByDictType(Integer dictType);
    List<DictItemVo> getList(Integer dictType);
    DictEntity getByJdbcType(Integer jdbcDataType, List<DictEntity> dictItemList);
    List<DictItemVo> getListFromCache(Integer dictType);
    String getDictValue(Integer dictType, Integer dictKey);
    Integer getDictKey(Integer dictType, String dictValue);
    Set<Integer> getDictKeySet(Integer dictType);
    Set<String> getDictValueSet(Integer dictType);
    boolean containsKey(Integer dictType, Integer dictKey);
    Map<Integer, Map<Integer, String>> getKeyToValMap();
    Map<Integer, Map<String, Integer>> getValToKeyMap();

}
