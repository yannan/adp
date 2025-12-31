package com.eisoo.metadatamanage.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eisoo.metadatamanage.web.service.IDictService;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;
import lombok.Data;

@Data
public class ExcelDictHandler implements IExcelDictHandler {
    private Map<String, List<Map>> listMap = null;
    private Map<String, Map<Integer, String>> dictKVMap = null;
    private Map<String, Map<String, Integer>> dictVKMap = null;

    public ExcelDictHandler(IDictService dictService, Map<Integer, String> dictType2NameMap) {
        this.listMap = new HashMap<>();
        this.dictKVMap = new HashMap<>();
        this.dictVKMap = new HashMap<>();
        Iterator<Entry<Integer, String>> dT2NEntries = dictType2NameMap.entrySet().iterator();
        while(dT2NEntries.hasNext()){
            Entry<Integer, String> dT2NEntry = dT2NEntries.next();
            Map<Integer, String> kvMap = dictService.getKeyToValMap().get(2);
            Map<String, Integer> vkMap = dictService.getValToKeyMap().get(2);

            this.dictKVMap.put(dT2NEntry.getValue(), kvMap == null ? new HashMap<>() : kvMap);
            this.dictVKMap.put(dT2NEntry.getValue(), vkMap == null ? new HashMap<>() : vkMap);

            List<Map> dictList = new ArrayList<>();
            if (kvMap != null) {
                Iterator<Entry<Integer, String>> valEntries = kvMap.entrySet().iterator();
                while(valEntries.hasNext()){
                    Entry<Integer, String> valEntry = valEntries.next();
                    Map<String, String> dMap = new HashMap<>();
                    dMap.put("dictKey", String.valueOf(valEntry.getKey()));
                    dMap.put("dictValue", valEntry.getValue());
                    dictList.add(dMap);
                }
            }
            listMap.put(dT2NEntry.getValue(), dictList);
        }
    }

    @Override
    public List<Map> getList(String dict) {
        List<Map> result = listMap.get(dict);
        return  result == null ? new ArrayList<>() : result;
    }

    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if (value != null) {
            Map<Integer, String> valMap = null;
            if ((valMap = dictKVMap.get(dict)) != null) {
                return valMap.get(Integer.parseInt(value.toString()));
            }
        }
        return null;
    }

    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if (value != null) {
            Map<String, Integer> valMap = null;
            if ((valMap = dictVKMap.get(dict)) != null) {
                return String.valueOf(valMap.get(value.toString()));
            }
        }
        return null;
    } 
}
