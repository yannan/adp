package com.eisoo.metadatamanage.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eisoo.metadatamanage.db.entity.TaskEntity;
import com.eisoo.metadatamanage.db.entity.TaskLogEntity;
import com.eisoo.metadatamanage.lib.dto.FillMetaDataDTO;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.metadatamanage.web.extra.service.virtualService.VirtualService;
import com.eisoo.metadatamanage.web.util.CheckErrorUtil;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.web.service.ITaskLogService;
import com.eisoo.metadatamanage.web.service.ITaskService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.StringUtil;
import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.controller
 * @Date: 2023/5/13 16:59
 */
@RestController
@RequestMapping("/v1/task")
@Validated
@Api(tags = "元数据采集任务管理")
public class TaskController {
    @Autowired
    ITaskService taskService;

    @Autowired
    ITaskLogService taskLogService;

    @ApiOperation(value = "01.数据源-通过数据源ID直连数据源采集元数据")
    @PostMapping(value = "/fillMetaData/{dsid}")
    public Result<?> fillMetaData(@PathVariable("dsid") String dsid) {
        CheckErrorUtil.checkPositiveLong(dsid, "dsid");
        return taskService.fillMetaData(dsid);
    }

    @ApiOperation(value = "07.数据源-通过数据源ID从虚拟化通道采集元数据")
    @PostMapping(value = "/fillMetaDataByVirtual/{taskId}")
    public Result<?> fillMetaDataByVirtual(@PathVariable("taskId") String taskId, @RequestHeader(value = "Authorization", required = true) String token) {
        CheckErrorUtil.checkUnauthorized(token);
        // 这里用于请求网关
        VirtualService.TOKEN = token;
        CheckErrorUtil.checkDsId(taskId);
        return taskService.fillMetaDataByVirtual(taskId);
    }

    @ApiOperation(value = "02.数据源-通过数据源名称采集元数据")
    @PostMapping(value = "/fillMetaData")
    public Result<?> fillMetaData(@Validated @RequestBody FillMetaDataDTO fillMetaDataDTO) {
        return taskService.fillMetaData(fillMetaDataDTO);
    }

    @ApiOperation(value = "08.数据源-通过数据源名称从虚拟化通道采集元数据")
    @PostMapping(value = "/fillMetaDataByVirtual")
    public Result<?> fillMetaDataByVirtual(@Validated @RequestBody FillMetaDataDTO fillMetaDataDTO) {
        return taskService.fillMetaDataByVirtual(fillMetaDataDTO);
    }

    @ApiOperation(value = "09.数据源-增量更新元数据")
    @PostMapping(value = "/updateMetaData")
    public Result<?> updateMetaData() {
        return taskService.updateMetaData();
    }

    @ApiOperation(value = "03.单表数据量更新", notes = "计算count(*),更新单表数据量")
    @PutMapping(value = "/getCount/{tableId}")
    public Result<?> updateRowNum(@PathVariable("tableId") Long tableId) {
        CheckErrorUtil.checkPositiveLong(tableId, "tableId");
        return taskService.updateRowNum(tableId);
    }

    @ApiOperation(value = "04.删除任务记录", notes = "删除任务记录")
    @DeleteMapping(value = "/{ids}")
    public Result<?> deleteByIds(@PathVariable("ids") String ids) {
        //校验结果
        CheckVo<String> checkVo = taskService.checkID(ids);
        //处理逻辑
        if (!StringUtils.isBlank(checkVo.getCheckCode())) {
            String description = "任务删除失败";
            throw new AiShuException(checkVo.getCheckCode(), description, checkVo.getCheckErrors());
        }

        //删除对应ID集合的任务记录
        List<Long> idList = StringUtil.splitNumByRegex(ids, ",");
        taskService.removeBatchByIds(idList);
        //删除对应ID集合的任务记录日志
        LambdaQueryWrapper<TaskLogEntity> taskLogEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskLogEntityLambdaQueryWrapper.in(TaskLogEntity::getTaskId, idList);
        taskLogService.remove(taskLogEntityLambdaQueryWrapper);
        return Result.success();
    }

    @ApiOperation(value = "05.任务-列表查询", notes = "查询任务列表")
    @GetMapping()
    public Result<List<TaskEntity>> getList(
            @RequestParam(value = "task_status", required = false)
            @Range(min = 0, max = 2, message = "task_status取值范围需满足[0,2]")
            Integer task_status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "start_time", required = false) String start_time,
            @RequestParam(value = "end_time", required = false) String end_time,
            @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "sort", required = false, defaultValue = "start_time") String sort,
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
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.DateFormatFailed);
        }


        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = keyword.replaceAll("_", "\\\\_");
            keyword = keyword.replaceAll("%", "\\\\%");
        }

        return taskService.getList(startTime, endTime, keyword, task_status, offset, limit, sort, direction);
    }

    @ApiOperation(value = "06.查询单个任务运行记录", notes = "简易实现，分布式环境下有问题")
    @GetMapping(value = "log/{id}")
    public Result<?> getLog(@PathVariable("id") Long taskId) {
        //校验结果
        CheckVo<TaskEntity> checkVo = taskService.checkID(taskId);
        //处理逻辑
        if (!StringUtils.isBlank(checkVo.getCheckCode())) {
            String description = "查询失败";
            throw new AiShuException(checkVo.getCheckCode(), description, checkVo.getCheckErrors());
        }
        return taskService.getLog(taskId);
    }

}
