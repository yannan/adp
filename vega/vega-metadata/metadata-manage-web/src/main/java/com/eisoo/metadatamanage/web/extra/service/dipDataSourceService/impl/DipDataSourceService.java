package com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.eisoo.metadatamanage.db.entity.DataSourceEntityDataConnection;
import com.eisoo.metadatamanage.db.mapper.DataConnectionDataSourceMapper;
import com.eisoo.metadatamanage.db.mapper.DipDataSourceMapper;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualConnectorListDto;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.metadatamanage.web.configuration.DpDataSourceConfiguration;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.IDipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.virtualService.VirtualService;
import com.eisoo.metadatamanage.web.handler.GlobalExceptionHandler;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.eisoo.metadatamanage.web.util.RSAUtil;
import com.eisoo.standardization.common.api.HttpResponseVo;
import com.eisoo.standardization.common.constant.Message;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DipDataSourceService implements IDipDataSourceService {
    @Autowired
    private DpDataSourceConfiguration dpDataSourceConfiguration;
    //    @Autowired
//    private DipDataSourceMapper dipDataSourceMapper;
    @Autowired(required = false)
    DataConnectionDataSourceMapper dataConnectionDataSourceMapper;

    @Override
    public synchronized  Boolean createConnector() {
        String collector = dpDataSourceConfiguration.getCollector();
        MPJLambdaWrapper<DataSourceEntityDataConnection> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(DataSourceEntityDataConnection::getFName, collector);
        DataSourceEntityDataConnection DataSourceEntityDataConnection = dataConnectionDataSourceMapper.selectOne(wrapper);
        // 根据需要，如果datasource里已经存在，就表示引擎已经有了这个数据源了
        if (null != DataSourceEntityDataConnection) {
            log.info("collectorApi对应的数据源：collector={}已经在引擎存在，不需要重复创建:datasource={}", collector, DataSourceEntityDataConnection);
            return true;
        }
        // /api/dp-data-source/v1/catalog
        String createConnectorUri = "%s://%s:%d%s";
        createConnectorUri = String.format(createConnectorUri,
                dpDataSourceConfiguration.getProtocol(),
                dpDataSourceConfiguration.getHost(),
                dpDataSourceConfiguration.getPort(),
                dpDataSourceConfiguration.getCatalogApi());
        log.info("createConnectorUri={}", createConnectorUri);
        HttpResponseVo responseVo = createMetadataConnector(createConnectorUri);
        return responseVo.isSucesss();
    }

    @Override
    public VirtualConnectorListDto getConnectors() {
        String virtualizationUri = "%s://%s:%d%s";
        String url = String.format(virtualizationUri,
                dpDataSourceConfiguration.getProtocol(),
                dpDataSourceConfiguration.getHost(),
                dpDataSourceConfiguration.getPort(),
                dpDataSourceConfiguration.getConnectorsApi());
        log.info("getConnectors的url:{}", url);
        String result = getResponseVo(url).getResult();
        return JSONUtils.json2Obj(result, VirtualConnectorListDto.class);
    }

    private HttpResponseVo getResponseVo(String url) {
        Header[] headers = new Header[1];
//        headers[0] = new BasicHeader("X-Presto-User", dpDataSourceConfiguration.getUser());
        // 这里添加token用于访问网关
        if (StringUtils.isNoneEmpty(VirtualService.TOKEN)) {
            headers[0] = new BasicHeader("Authorization", VirtualService.TOKEN);
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

    private HttpResponseVo createMetadataConnector(String url) {
        HashMap<String, String> headers = new HashMap();
        // 这里添加token用于访问网关
        if (StringUtils.isNoneEmpty(VirtualService.TOKEN)) {
            log.info("token不是空:{}", VirtualService.TOKEN);
            headers.put("Authorization", VirtualService.TOKEN);
        }
//        headers.put("Authorization", "Bearer ory_at_JxnwCA8Y_3ze0hy8N_qlXO9dY9o2JV9HmHr4KLFTEGI.OET3K2dXzhE1fpScVrNv7qhIxOglaWN_4NoiKmmvyng");
        HttpResponseVo responseVo = null;
        JSONObject jsonObject = new JSONObject();
        try {
            String metaDataConnectorType = dpDataSourceConfiguration.getMetaDataConnector().toLowerCase();
            if (metaDataConnectorType.contains("maria")) {
                metaDataConnectorType = "maria";
            }else if (metaDataConnectorType.contains("dm")) {
                metaDataConnectorType = "dameng";
            }
            // name:数据源名称
            jsonObject.put("name", dpDataSourceConfiguration.getCollector());
            jsonObject.put("type", metaDataConnectorType);
            jsonObject.put("comment", "");
            JSONObject binData = new JSONObject();
            binData.put("database_name", "adp");
            binData.put("connect_protocol", "jdbc");
            binData.put("host", dpDataSourceConfiguration.getMetaDataHost());
            binData.put("port", dpDataSourceConfiguration.getMetaDataPort());
            binData.put("password", RSAUtil.encrypt(dpDataSourceConfiguration.getMetaPassword()));
            binData.put("account", dpDataSourceConfiguration.getMetaUser());
            jsonObject.put("bin_data", binData);
            log.info("【扫描元数据：createMetadataConnector】 请求网关:url={},body={},headers={}", url, jsonObject, headers);
            responseVo = HttpUtil.executePost(url, JSONObject.toJSONString(jsonObject), headers);
        } catch (Exception e) {
            log.error("【扫描元数据：createMetadataConnector】 请求网关失败:responseVo={}", responseVo, e);
            throw GlobalExceptionHandler.getNewAishuException(ErrorCodeEnum.Invalid, e.toString());
        }
        if (isSuccess(responseVo)) {
            return responseVo;
        } else {
            throw GlobalExceptionHandler.getNewAishuException(ErrorCodeEnum.Invalid, Message.MESSAGE_PARAM_ERROR_SOLUTION);
        }
    }
    private Boolean isSuccess(HttpResponseVo responseVo) {
        if (AiShuUtil.isEmpty(responseVo)) {
            return false;
        }
        String respData = responseVo.getResult();
        Integer codeNum = responseVo.getCode();
        if (codeNum < 200 || codeNum > 300) {
            log.error("responseVo 状态码：{},responseVo:{}", codeNum, responseVo);
            Map<String, Object> respDataMap = JSON.parseObject(respData, Map.class);
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
}
