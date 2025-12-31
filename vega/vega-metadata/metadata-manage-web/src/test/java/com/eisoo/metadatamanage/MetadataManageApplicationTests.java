package com.eisoo.metadatamanage;

import com.eisoo.entity.LineageOpLogEntity;
import com.eisoo.mapper.LineageOpLogMapper;
import com.eisoo.mapper.RelationMapper;
import com.eisoo.metadatamanage.db.entity.DipDataSourceEntity;
import com.eisoo.metadatamanage.web.configuration.DruidPoolConfig;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.IDipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.virtualService.VirtualService;
import com.eisoo.metadatamanage.web.service.IVegaDataSourceService;
import com.eisoo.metadatamanage.web.service.impl.lineage.platform.PlatFormLineageInitService;
import com.eisoo.service.impl.DolphinLineageServiceImpl;
import com.eisoo.service.impl.RelationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.eisoo.metadatamanage.web.MetadataManageApplication;
import com.eisoo.metadatamanage.db.mapper.DataSourceMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = {MetadataManageApplication.class})
class MetadataManageApplicationTests {
    @Autowired
    private PlatFormLineageInitService platFormLineageInitService;
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private RelationService relationService;
    @Autowired
    private DataSourceMapper dataSourceMapper;
    @Autowired
    private LineageOpLogMapper lineageOpLogMapper;
    // DolphinLineageServiceImpl
    @Autowired
    private DolphinLineageServiceImpl dolphinLineageServiceImpl;
    @Autowired
    IVegaDataSourceService vegaDataSourceService;
    @Autowired
    VirtualService virtualService;
    @Autowired
    IDipDataSourceService dipDataSourceService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Resource
    private DruidPoolConfig druidPoolConfig;
    @Test
    void testLineageOpLogMapper() {
        ArrayList<LineageOpLogEntity> list = new ArrayList<>();
        LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity("table", "insert", "{}");
        lineageOpLogEntity.setClassId("abc");
        list.add(lineageOpLogEntity);
        lineageOpLogMapper.insertBatchSomeColumn(list);
    }

    @Test
    void testInsertBatchRelation() throws Exception {
        platFormLineageInitService.insertBatchTable();
        platFormLineageInitService.insertBatchColumn();
        platFormLineageInitService.insertBatchIndicator();
    }

    //     relationService.initInsertBatchChildRelationColumn(BATCH_SIZE);
    @Test
    void testInsertBatchChildRelationColumn() throws Exception {
        platFormLineageInitService.insertBatchTable();
        platFormLineageInitService.insertBatchColumn();
        platFormLineageInitService.insertBatchIndicator();
        // 同步relation数据
        platFormLineageInitService.insertBatchRelation();
    }

    @Test
    void testDolphinLineageInitService() throws Exception {
        platFormLineageInitService.insertBatchTable();
        platFormLineageInitService.insertBatchColumn();
        platFormLineageInitService.insertBatchIndicator();
        // 同步relation数据
        platFormLineageInitService.insertBatchRelation();
        // 同步dolphin数据
    }
    @Test
    void testDataSourceMapper() {
        List<String> dataSource = dataSourceMapper.getDataSourceList("MYSQL", "10.4.134.47", 3330, "dmw");
        System.out.println(dataSource);
    }
    @Test
    void testInsertBatchTable() throws Exception {
        platFormLineageInitService.insertBatchTable();
    }
    @Test
    void testInitSyncDolphin() {
        dolphinLineageServiceImpl.initSyncDolphin();
    }
    // initComposeDolphin
    @Test
    void testInitComposeDolphin() {
        dolphinLineageServiceImpl.initComposeDolphin();
    }
    @Test
    void testDipDataSourceEntity() {
        DipDataSourceEntity dataSourceEntityVega = vegaDataSourceService.getByDataSourceId("12f7f6d2-0690-4787-bce8-d96ba8d2a78d");
        String s = new String(dataSourceEntityVega.getBinData());
        System.out.println(s);
        //{"catalog_name":"maria_ftm53ns9","data_view_source":"vdm_maria_ftm53ns9.default","database_name":"af_main","connect_protocol":"jdbc","host":"10.4.110.188","port":3330,"account":"root","password":"rIhhTYPRWzYpxM7bm//XcyPD1S4A1ptB0KUy9Z/+sq5XMrptWdc0fIItR1JcTZRZjPOlV/lcsqz1hgWvDpMK6SyEmy3LBfKBSL9iO0R/DBwWU31sUUauneE0L/95vZM5JJCwfMOdv8mZj3xTblpECDmEq712ADJxQ7gfYfX9bVQ="}
    }

    @Test
    void testJdbcTemplate() {
        Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap("select * from t_table limit 1");
        System.out.println(stringObjectMap.toString());
    }
    @Test
    void testCreateConnector() {
        dipDataSourceService.createConnector();
    }

}
