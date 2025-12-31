package com.eisoo.metadatamanage.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eisoo.dto.build.NetWorkBuildDto;
import com.eisoo.entity.GraphInfoEntity;
import com.eisoo.metadatamanage.web.commons.GraphUtil;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.config.DataSourceBuildConfig;
import com.eisoo.metadatamanage.web.service.IAdGraphBuildService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Set;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/22 10:14
 * @Version:1.0
 */
@Service
@Slf4j
public class AdGraphBuildServiceImpl implements IAdGraphBuildService {
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Autowired
    private DataSourceBuildConfig dataSourceBuildConfig;
    @Autowired
    private GraphInfoServiceImpl graphInfoServiceImpl;
    @Override
    public void start(NetWorkBuildDto netWorkBuildDto) {
        String knwName = netWorkBuildDto.getKnwName();
        try {
            // 根据图谱名字获取图谱id
            int knwId = GraphUtil.getNetWorkKnwIdByName(knwName,anyDataGraphConfig);
            if (-99 == knwId) {
                log.warn("{} not existed! now start build this network!", knwName);
                GraphUtil.buildNetWork(netWorkBuildDto,anyDataGraphConfig);
            } else {
                log.info("{} already existed ! no need rebuild this network!", knwName);
                anyDataGraphConfig.setKnwId(knwId);
            }
            //1,获取所有的图谱
            HashMap<String, Integer> allGraphs = GraphUtil.getAllGraphs(anyDataGraphConfig);
            //2,删除所有的图谱
            Set<String> set = allGraphs.keySet();
            for (String graphName : set) {
                Integer graphId = allGraphs.get(graphName);
                boolean b = GraphUtil.deleteGraphById(graphId, anyDataGraphConfig);
                if (b) {
                    log.warn("删除图谱：{} 成功", graphName);
                }
            }
            GraphUtil.startBuildDataSource(anyDataGraphConfig,dataSourceBuildConfig);
            GraphUtil.startBuildGraph(anyDataGraphConfig);
            // 更新到mysql中去
            UpdateWrapper<GraphInfoEntity> queryWrapper = new UpdateWrapper<>();
            queryWrapper.eq("app_id", anyDataGraphConfig.getAppId());
            GraphInfoEntity graphInfoEntity = new GraphInfoEntity();
            Integer graphId = anyDataGraphConfig.getGraphId();
            graphInfoEntity.setGraphId(graphId);
            boolean update = graphInfoServiceImpl.update(graphInfoEntity, queryWrapper);
            if (update) {
                log.info("更新graphId:{}成功！", graphId);
            } else {
                log.error("更新graphId:{}失败！", graphId);
                throw new Exception();
            }
            log.info("build up load graph json file success ! ");
            GraphUtil.startGraphService(anyDataGraphConfig);
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                     "构建图谱失败！",
                                     e.getMessage());
        }
    }
}


