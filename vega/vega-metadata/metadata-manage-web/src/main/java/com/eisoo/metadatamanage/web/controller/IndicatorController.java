package com.eisoo.metadatamanage.web.controller;

import com.eisoo.metadatamanage.lib.dto.IndicatorCreateDTO;
import com.eisoo.metadatamanage.db.entity.IndicatorEntity;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.metadatamanage.web.service.IIndicatorService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.StringUtil;
import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import java.util.Date;
import java.util.List;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.controller
 * @Date: 2023/5/10 18:04
 */
@Api(tags="指标统计")
@RestController
@RequestMapping("/v1/indicator")
@Slf4j
public class IndicatorController {
    @Autowired
    IIndicatorService indicatorService;

    @ApiOperation(value  = "01.指标统计内置任务")
    @PostMapping(value = "/innerTask")
    public Result<?> InnerTask(){
        return indicatorService.innerTask();
    }

    @ApiOperation(value = "02.指标-新建", notes = "接收第三方或手工创建指标")
    @PostMapping()
    public Result<?> add(@Validated @RequestBody IndicatorCreateDTO req) {
        indicatorService.create(req);
        return Result.success();
    }

    @ApiOperation(value = "03.删除指标记录", notes = "按时间范围")
    @DeleteMapping()
    public Result<?> deleteByTime( @RequestParam(value = "start_time") String start_time,
                                   @RequestParam(value = "end_time") String end_time){
        Date startTime;
        Date endTime;
        try {
            startTime = ConvertUtil.toDate(start_time);
            endTime = ConvertUtil.toDate(end_time);
        } catch (Exception e){
            throw new AiShuException(ErrorCodeEnum.DateFormatFailed);
        }
        indicatorService.deleteByTime(startTime, endTime);
        return Result.success();
    }

    @ApiOperation(value = "04.删除指标记录", notes = "按ID")
    @DeleteMapping(value = "/{ids}")
    public Result<?> deleteByIds( @PathVariable("ids") String ids){
        //校验结果
        CheckVo<String> checkVo = indicatorService.checkID(ids);
        //处理逻辑
        if (!StringUtils.isBlank(checkVo.getCheckCode())) {
            String description = "指标删除失败";
            throw new AiShuException(checkVo.getCheckCode(),description,checkVo.getCheckErrors());
        }

        //ID集合
        List<Long> idList = StringUtil.splitNumByRegex(ids, ",");
        indicatorService.removeBatchByIds(idList);
        return Result.success();
    }

    @ApiOperation(value = "05.指标-列表查询", notes = "查询指标列表")
    @GetMapping()
    public Result<List<IndicatorEntity>> getList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "start_time", required = false) String start_time,
            @RequestParam(value = "end_time", required = false) String end_time,
            @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction
    ) {
        if (offset == null || offset < 1) {
            offset = 1;
        }
        if (limit == null || limit <= 0) {
            limit = 20;
        } else if (limit > 1000) {
            limit = 1000;
        }

        Date startTime;
        Date endTime;

        try {
            startTime = ConvertUtil.toDate(start_time);
            endTime = ConvertUtil.toDate(end_time);
        } catch (Exception e){
            throw new AiShuException(ErrorCodeEnum.DateFormatFailed);
        }

        if (!Strings.isNullOrEmpty(type)) {
            type = StringUtils.substring(type,0,128);
            type = StringUtil.escapeSqlSpecialChars(type);
        }
        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = StringUtils.substring(keyword,0,128);
            keyword = StringUtil.escapeSqlSpecialChars(keyword);
        }
        return indicatorService.getList(startTime, endTime, keyword, type, offset, limit, direction);
    }

}
