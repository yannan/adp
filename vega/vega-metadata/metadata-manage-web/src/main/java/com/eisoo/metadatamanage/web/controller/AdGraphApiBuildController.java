package com.eisoo.metadatamanage.web.controller;

import com.eisoo.metadatamanage.web.service.impl.lineage.platform.PlatFormLineageInitService;
import com.eisoo.service.impl.LineageOpLogService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:17
 * @Version:1.0
 */
@RestController
@RequestMapping("/v1/build")
@Slf4j
public class AdGraphApiBuildController {
    @Autowired
    private PlatFormLineageInitService platFormLineageInitService;
    @Autowired
    private LineageOpLogService lineageOpLogService;

    @ResponseBody
    @PutMapping("/startAdGraphBuild")
    public Result<?> startAdGraphBuild(@RequestParam(value = "isReBuildDataSource", required = false, defaultValue = "false") String isReBuildDataSource) throws Exception {
        if (!"false".equals(isReBuildDataSource) && !"true".equals(isReBuildDataSource)) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter,"参数isReBuildDataSource错误！","请检查：参数isReBuildDataSource的值只能是false或者true，或者不填默认就是false");
        }
        // TODO:这里改成启动自动初始化了，接口的方式先搁置，后面如果有需要再开放
//        boolean reBuild = Boolean.parseBoolean(isReBuildDataSource);
//        if (reBuild) {
//            platFormLineageInitService.insertBatchTable();
//            platFormLineageInitService.insertBatchColumn();
//            platFormLineageInitService.insertBatchIndicator();
//            // 同步dolphin数据
//            platFormLineageInitService.insertBatchDolphin();
//            // 同步relation数据
//            platFormLineageInitService.insertBatchRelation();
//            // 记录日志
//            lineageOpLogService.saveInitDataToLog();
//        }
        // TODO:向ad构建图谱暂时搁置
//        NetWorkBuildDto netWorkBuildDto = new NetWorkBuildDto(anyDataGraphConfig.getGraphName(), anyDataGraphConfig.getGraphDesc());
//        adGraphBuildServiceImpl.start(netWorkBuildDto);
        return Result.success();
    }
}
