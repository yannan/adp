package com.eisoo.metadatamanage.web.controller;

import com.eisoo.metadatamanage.lib.dto.LineageReportDto;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.web.service.ILineageService;
import com.eisoo.metadatamanage.web.util.CheckErrorUtil;
import com.eisoo.metadatamanage.web.util.CheckErrorVo;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.AiShuUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "血缘")
@RestController
@RequestMapping("/v1/lineage")
@Slf4j
public class LineageController {


    @Autowired
    ILineageService lineageService;


    @ApiOperation(value = "01.血缘主动上报接口")
    @PostMapping(value = "/report")
    public Result<?> report(@Validated @RequestBody LineageReportDto reportDto) {

        List<CheckErrorVo> checkErrorVoList = new ArrayList<>();
        int idx = 0;
        for (LineageReportDto.Lineage row : reportDto.getData()) {
            checkDbType(checkErrorVoList, idx, row);
            checkDatabaseSchema(checkErrorVoList, idx, row);
            idx++;
        }
        if (!checkErrorVoList.isEmpty()) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, checkErrorVoList);
        }
        lineageService.report(reportDto);
        return Result.success();
    }

    private static void checkDbType(List<CheckErrorVo> checkErrorVoList, int idx, LineageReportDto.Lineage row) {
        LineageReportDto.Lineage.Column source = row.getSource();
        if (DbType.of(source.getDbType()) == null) {
            String key = String.format("data[%s].source.db_type", idx);
            CheckErrorUtil.createError(key, "数据库类型填写不正确或者为空", checkErrorVoList);
        }

        LineageReportDto.Lineage.Column target = row.getTarget();
        if (DbType.of(target.getDbType()) == null) {
            String key = String.format("data[%s].target.db_type", idx);
            CheckErrorUtil.createError(key, "数据库类型填写不正确或者为空", checkErrorVoList);
        }
    }

    private static void checkDatabaseSchema(List<CheckErrorVo> checkErrorVoList, int idx, LineageReportDto.Lineage row) {
        LineageReportDto.Lineage.Column source = row.getSource();
        if (AiShuUtil.isEmpty(source.getDbName()) && AiShuUtil.isEmpty(source.getDbSchema())) {
            String key = String.format("data[%s].source.db_name", idx);
            CheckErrorUtil.createError(key, "数据库名称和shcema不能同时为空", checkErrorVoList);

            key = String.format("data[%s].source.db_schema", idx);
            CheckErrorUtil.createError(key, "数据库名称和shcema不能同时为空", checkErrorVoList);
        }

        LineageReportDto.Lineage.Column target = row.getTarget();
        if (AiShuUtil.isEmpty(target.getDbName()) && AiShuUtil.isEmpty(target.getDbSchema())) {
            String key = String.format("data[%s].target.db_name", idx);
            CheckErrorUtil.createError(key, "数据库名称和shcema不能同时为空", checkErrorVoList);

            key = String.format("data[%s].target.db_schema", idx);
            CheckErrorUtil.createError(key, "数据库名称和shcema不能同时为空", checkErrorVoList);
        }
    }

}
