package com.eisoo.metadatamanage.web.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eisoo.entity.GraphInfoEntity;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.IDipDataSourceService;
import com.eisoo.metadatamanage.web.service.impl.GraphInfoServiceImpl;
import com.eisoo.metadatamanage.web.service.impl.lineage.platform.PlatFormLineageInitService;
import com.eisoo.service.impl.LineageOpLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/13 17:54
 * @Version:1.0
 */
@Component
@Slf4j
public class MyApplicationReadyListener implements ApplicationListener<ContextRefreshedEvent> {
    public final static Integer SUCCESS = 0;
    public final static Integer FAIL = 1;
    public final static String TABLE_INIT_ID = "1";
    public final static String COLUMN_INIT_ID = "2";
    public final static String INDICATOR_INIT_ID = "3";
    public final static String DOLPHIN_INIT_ID = "4";
    public final static String RELATION_INIT_ID = "5";
    public final static String LOG_INIT_ID = "6";
    @Autowired
    private GraphInfoServiceImpl graphInfoServiceImpl;
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Autowired
    private PlatFormLineageInitService platFormLineageInitService;
    @Autowired
    private LineageOpLogService lineageOpLogService;
    @Autowired
    IDipDataSourceService dipDataSourceService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //  initGraphTask();
//        Integer InitMaxCount = anyDataGraphConfig.getInitMaxCount();
//        boolean result = true;
//        // table
//        if (result) {
//            GraphInfoEntity table = graphInfoServiceImpl.getById(TABLE_INIT_ID);
//            if (table == null || FAIL.equals(table.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = platFormLineageInitService.insertBatchTable();
//                    if (result) {
//                        break;
//                    }
//                }
//                // 更新数据库
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(TABLE_INIT_ID, result ? SUCCESS : FAIL));
//            }
//        }
//        // column
//        if (result) {
//            GraphInfoEntity column = graphInfoServiceImpl.getById(COLUMN_INIT_ID);
//            if (column == null || FAIL.equals(column.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = platFormLineageInitService.insertBatchColumn();
//                    if (result) {
//                        break;
//                    }
//                }
//                // 更新数据库
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(COLUMN_INIT_ID, result ? SUCCESS : FAIL));
//            }
//        }
//        // indicator
//        if (result) {
//            GraphInfoEntity indicator = graphInfoServiceImpl.getById(INDICATOR_INIT_ID);
//            if (indicator == null || FAIL.equals(indicator.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = platFormLineageInitService.insertBatchIndicator();
//                    if (result) {
//                        break;
//                    }
//                }
//                // 更新数据库
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(INDICATOR_INIT_ID, result ? SUCCESS : FAIL));
//            }
//        }
//        // 同步dolphin数据
//        if (result) {
//            GraphInfoEntity dolphin = graphInfoServiceImpl.getById(DOLPHIN_INIT_ID);
//            if (dolphin == null || FAIL.equals(dolphin.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = platFormLineageInitService.insertBatchDolphin();
//                    if (result) {
//                        break;
//                    }
//                }
//                // 更新数据库
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(DOLPHIN_INIT_ID, result ? SUCCESS : FAIL));
//                // 因为column数据被污染了，所以相关的也要更新
//                if (!result) {
//                    graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(COLUMN_INIT_ID, FAIL));
//                    graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(RELATION_INIT_ID, FAIL));
//                    graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(LOG_INIT_ID, FAIL));
//                }
//            }
//        }
//        // 同步relation数据
//        if (result) {
//            GraphInfoEntity relation = graphInfoServiceImpl.getById(RELATION_INIT_ID);
//            if (relation == null || FAIL.equals(relation.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = platFormLineageInitService.insertBatchRelation();
//                    if (result) {
//                        break;
//                    }
//                }
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(RELATION_INIT_ID, result ? SUCCESS : FAIL));
//            }
//        }
//        // 记录日志
//        if (result) {
//            GraphInfoEntity log = graphInfoServiceImpl.getById(LOG_INIT_ID);
//            if (log == null || FAIL.equals(log.getGraphId())) {
//                // 需要初始化
//                for (int i = 0; i < InitMaxCount; i++) {
//                    result = lineageOpLogService.saveInitDataToLog();
//                    if (result) {
//                        break;
//                    }
//                }
//                graphInfoServiceImpl.insertOrUpdate(new GraphInfoEntity(LOG_INIT_ID, result ? SUCCESS : FAIL));
//            }
//        }
//        if (!result) {
//            log.error("由于血缘数据初始化失败，因此无法完成启动，请检查");
//            throw new RuntimeException();
//        }
//        if (result) {
//            log.info("---【血缘数据初始化完成】!初始化工作结束!---");
//        }
    }

    /**
     * 图谱初始化相关工作
     */
    private void initGraphTask() {
        QueryWrapper<GraphInfoEntity> queryWrapper = new QueryWrapper<>();
        String appId = anyDataGraphConfig.getAppId();
        if (CommonUtil.isEmpty(appId)) {
            log.error("SpringBoot启动完成，但是app_id是空！请检查！");
            throw new RuntimeException();
        }
        queryWrapper.eq("app_id", appId);
        boolean b = graphInfoServiceImpl.getOne(queryWrapper) != null;
        if (!b) {
            GraphInfoEntity graphInfoEntity = new GraphInfoEntity();
            graphInfoEntity.setAppId(appId);
            boolean save = graphInfoServiceImpl.save(graphInfoEntity);
            if (save) {
                log.info("app_id={}插入成功！", appId);
            } else {
                log.error("appId插入mysql失败");
                throw new RuntimeException();
            }
        }
    }
}