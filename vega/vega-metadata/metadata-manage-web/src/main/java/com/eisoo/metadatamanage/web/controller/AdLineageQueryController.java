package com.eisoo.metadatamanage.web.controller;
import com.eisoo.dto.AdLineageQueryDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.metadatamanage.web.service.impl.AdLineageQueryServiceImpl;
import com.eisoo.metadatamanage.web.service.impl.lineage.AnyDataLineageServiceManager;
import com.eisoo.service.ILineageService;
import com.eisoo.service.impl.ColumnLineageServiceImpl;
import com.eisoo.service.impl.RelationService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.util.JsonUtils;
import com.eisoo.util.LineageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:17
 * @Version:1.0
 */
@RestController
@RequestMapping("/v1/queryService")
@Slf4j
public class AdLineageQueryController {
    @Autowired
    private AdLineageQueryServiceImpl adLineageQueryServiceImpl;
    @Autowired
    private RelationService relationService;
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;

    @GetMapping(value = "/getLineageData")
    public Result<List<? extends BaseLineageEntity>> getLineageData(@RequestParam(value = "type", required = false) String type,
                                                                    @RequestParam(value = "ids", required = false) List<String> ids) {
        ILineageService iLineageService = AnyDataLineageServiceManager.LINEAGE_SERVICE_MAP.get(type);
        List<? extends BaseLineageEntity> baseLineageEntities = iLineageService.selectBatchIds(ids);
        if (null == baseLineageEntities){
            baseLineageEntities = new ArrayList<>(1);
        }
        return Result.success(baseLineageEntities);
    }

    @GetMapping(value = "/getLineageColumns/{id}")
    public Result<String> getLineageColumns(@PathVariable(value = "id") String id) {
        String columnList = columnLineageServiceImpl.getColumnList(id);
        return Result.success(columnList);
    }

    @GetMapping(value = "/getLineageRelationData")
    public Result<HashMap<String, List<Object>>> getLineageRelationData(@RequestParam(value = "ids") List<String> ids,
                                                                        @RequestParam(value = "direction") String direction,
                                                                        @RequestParam(value = "step", required = false, defaultValue = "2") Integer step) {
        HashMap<String, List<Object>> result = new HashMap<>();
        for (String id : ids) {
            List<Object> list = new ArrayList<>();
            list = relationService.makeRelationData(list, id, step, direction);
            result.put(id, list);
        }
        return Result.success(result);
    }

    @GetMapping(value = "/getAdLineage")
    public Result<String> getAdLineage(@RequestParam(value = "id") String id,
                                       @RequestParam(value = "type") String type,
                                       @RequestParam(value = "direction") String direction,
                                       @RequestParam(value = "step", required = false) String step) {
        // 检测参数
        LineageUtil.checkGetAdLineageParams(id, type, direction, step);
        AdLineageQueryDto adLineageQueryDto = new AdLineageQueryDto(id, type, direction, step);
        log.info("查询ad参数:{}", JsonUtils.toJsonString(adLineageQueryDto));
        String response = adLineageQueryServiceImpl.getAdLineage(adLineageQueryDto);
        JsonNode jsonNode = JsonUtils.toJsonNode(response);
        assert jsonNode != null;
        if (jsonNode.hasNonNull("res")) {
            JsonNode nodeRes = jsonNode.get("res");
            if (null != nodeRes) {
                return Result.success(nodeRes.toString());
            }
            return Result.success(null);
        }
        Result<String> result = new Result<>();
        result.setCode("-1");
        result.setDescription(response);
        return result;
    }
}
