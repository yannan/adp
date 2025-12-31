package com.eisoo.metadatamanage.web.extra.service.virtualService;


import com.alibaba.fastjson2.JSONObject;
import com.eisoo.metadatamanage.db.entity.DataSourceEntityDataConnection;
import com.eisoo.metadatamanage.db.entity.DipDataSourceEntity;
import com.eisoo.metadatamanage.db.mapper.DataConnectionDataSourceMapper;
import com.eisoo.metadatamanage.db.mapper.DipDataSourceMapper;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualCatalogListDto;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualColumnListDto;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualConnectorListDto;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualTableListDto;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.metadatamanage.web.configuration.DpDataSourceConfiguration;
import com.eisoo.metadatamanage.web.configuration.VirtualizationConfiguration;
import com.eisoo.metadatamanage.web.handler.GlobalExceptionHandler;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.eisoo.standardization.common.api.HttpResponseVo;
import com.eisoo.standardization.common.constant.Message;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VirtualService {
    public static String TOKEN = "";
    @Autowired
    VirtualizationConfiguration virtualizationConfiguration;
    @Autowired
    private DpDataSourceConfiguration dpDataSourceConfiguration;
    @Autowired
    DipDataSourceMapper dipDataSourceMapper;
    @Autowired
    DataConnectionDataSourceMapper dataConnectionDataSourceMapper;


    //    public VirtualCatalogListDto getCataLog() {
//        return JSONUtils.json2Obj(getResponseVo(getCatalogApiUrl()).getResult(), VirtualCatalogListDto.class);
//    }
    private HttpResponseVo getSchemaByApi(String catalog) {
        String url = getSchemaApiUrl(catalog);
        return getResponseVo(url);
    }

    public List<String> getSchema(String catalog) {
        return JSONUtils.json2List(getSchemaByApi(catalog).getResult(), String.class);
    }

    private HttpResponseVo getTableByApi(String catalog, String schema) {
        String url = getTableApiUrl(catalog, schema);
        return getResponseVo(url);
    }

    public VirtualTableListDto getTable(String catalog, String schema) {
        String result = getTableByApi(catalog, schema).getResult();
        return JSONUtils.json2Obj(result, VirtualTableListDto.class);
    }

    private HttpResponseVo getColumnByApi(String catalog, String schema, String table) {
        String url = getColumnApiUrl(catalog, schema, table);
        return getResponseVo(url);
    }

    public String collectMetadata(String catalog, String schema, String datasourceId, Long schemaId, String taskId) {
        return collectMetadataByApi(catalog, schema, datasourceId, schemaId, taskId).getResult();
    }

    private HttpResponseVo collectMetadataByApi(String catalog, String schema, String datasourceId, Long schemaId, String taskId) {
        String url = getCollectorUrl(catalog, schema, datasourceId, schemaId, taskId);
        return getResponseVo(url);
    }


    public VirtualColumnListDto getColumn(String catalog, String schema, String table) {
        return JSONUtils.json2Obj(getColumnByApi(catalog, schema, table).getResult(), VirtualColumnListDto.class);
    }

    private HttpResponseVo getResponseVo(String url) {
        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("X-Presto-User", virtualizationConfiguration.getUser());
        // 这里添加token用于访问网关
        if (StringUtils.isNoneEmpty(TOKEN)) {
            headers[1] = new BasicHeader("Authorization", TOKEN);
        }
        HttpResponseVo responseVo;
        try {
            responseVo = HttpUtil.httpGet(url, null, headers);
        } catch (Exception e) {
            throw GlobalExceptionHandler.getNewAishuException(ErrorCodeEnum.Invalid, e.toString());
        }
        if (isSuccess(responseVo)) {
            return responseVo;
        } else {
            throw GlobalExceptionHandler.getNewAishuException(ErrorCodeEnum.Invalid, Message.MESSAGE_PARAM_ERROR_SOLUTION);
        }
    }

    public String getCollectorUrl(String catalog, String schema, String datasourceId, Long schemaId, String taskId) {
        String virtualizationUri = "%s://%s:%d/api/virtual_engine_service%s?datasourceId=%s&schemaId=%s&taskId=%s";
        String url = String.format(virtualizationUri, virtualizationConfiguration.getProtocol(),
                virtualizationConfiguration.getHost(),
                virtualizationConfiguration.getPort(),
                virtualizationConfiguration.getCollectorApi(), ///v1/metadata/tables/%s/%s/%s
                datasourceId, schemaId, taskId);
        // 这里要从datasource查出来:http://localhost:8099/api/virtual_engine_service/v1/metadata/tables/%s/%s/%s?datasourceId=12345&schemaId=null&taskId=abc
        String collector = dpDataSourceConfiguration.getCollector();
//        MPJLambdaWrapper<DipDataSourceEntity> wrapper = new MPJLambdaWrapper<>();
//        wrapper.eq(DipDataSourceEntity::getName, collector);
//        DipDataSourceEntity dipDataSourceEntity = dipDataSourceMapper.selectOne(wrapper);
//        JSONObject jsonProperty = null;
//        try {
//            jsonProperty = JSONObject.parse(new String(dipDataSourceEntity.getBinData()));
//        } catch (Exception e) {
//            log.error("获取数据源的binData失败，dataSourceEntity={}", dipDataSourceEntity, e);
//            throw new AiShuException(ErrorCodeEnum.UnKnowException);
//        }
//        String catalogName = jsonProperty.getString("catalog_name").toLowerCase();
        MPJLambdaWrapper<DataSourceEntityDataConnection> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(DataSourceEntityDataConnection::getFName, collector);
        DataSourceEntityDataConnection dipDataSourceEntity = dataConnectionDataSourceMapper.selectOne(wrapper);
        String catalogName = dipDataSourceEntity.getFCatalog();
        log.info("collectorApi对应的collector={}；catalog={}", collector, catalogName);
        url = String.format(url, catalogName, catalog, schema);
        return url;
    }

    private Boolean isSuccess(HttpResponseVo responseVo) {
        if (AiShuUtil.isEmpty(responseVo)) {
            return false;
        }
        String respData = responseVo.getResult();
        Integer codeNum = responseVo.getCode();
        if (codeNum < 200 || codeNum > 300) {
            log.error("responseVo 状态码：{},responseVo:{}", codeNum, responseVo);
            Map<String, Object> respDataMap = JSONUtils.json2Obj(respData, Map.class);
            if (AiShuUtil.isNotEmpty(respDataMap) && AiShuUtil.isNotEmpty(respDataMap.get("code"))) {
                String code = String.valueOf(respDataMap.get("code"));
                String description = String.valueOf(respDataMap.get("description"));
                String detail = String.valueOf(respDataMap.get("detail"));
                String solution = String.valueOf(respDataMap.get("solution"));
                throw GlobalExceptionHandler.getNewAishuException(code, description, detail, solution);
            } else {
                return false;
            }
        }
        return true;
    }

    public String getSchemaApiUrl(String catalog) {
        String virtualizationUri = "%s://%s:%d/api/virtual_engine_service%s";
        String url = String.format(virtualizationUri, virtualizationConfiguration.getProtocol(), virtualizationConfiguration.getHost(), virtualizationConfiguration.getPort(), virtualizationConfiguration.getSchemaApi());
        url = String.format(url, catalog);
        return url;
    }

    public String getTableApiUrl(String catalog, String schema) {
        String virtualizationUri = "%s://%s:%d/api/virtual_engine_service%s";
        String url = String.format(virtualizationUri,
                virtualizationConfiguration.getProtocol(),
                virtualizationConfiguration.getHost(),
                virtualizationConfiguration.getPort(),
                virtualizationConfiguration.getTableApi()
        );
        url = String.format(url, catalog, schema);
        return url;
    }

    public String getColumnApiUrl(String catalog, String schema, String table) {
        String virtualizationUri = "%s://%s:%d/api/virtual_engine_service%s";
        String url = String.format(virtualizationUri, virtualizationConfiguration.getProtocol(), virtualizationConfiguration.getHost(), virtualizationConfiguration.getPort(), virtualizationConfiguration.getColumnApi());
        url = String.format(url, catalog, schema, table);
        return url;
    }

    public String getDsTypeUrl(String catalog, String schema, String table) {
        String virtualizationUri = "%s://%s:%d/api/virtual_engine_service%s";
        String url = String.format(virtualizationUri, virtualizationConfiguration.getProtocol(), virtualizationConfiguration.getHost(), virtualizationConfiguration.getPort(), virtualizationConfiguration.getColumnApi());
        url = String.format(url, catalog, schema, table);
        return url;
    }
}
